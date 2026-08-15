package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.mappers.ProductInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductBloomFilterComponentTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private ProductInfoMapper<?, ?> productInfoMapper;
    @Mock
    private RBloomFilter<String> bloomFilter;

    @InjectMocks
    private ProductBloomFilterComponent productBloomFilterComponent;

    @Test
    void add_emptyProductId_ignored() {
        productBloomFilterComponent.add("");
        productBloomFilterComponent.add(null);

        verifyNoInteractions(redissonClient);
    }

    @Test
    void add_initializesAndAdds() {
        when(redissonClient.getBloomFilter(Constants.REDIS_KEY_PRODUCT_BLOOM)).thenReturn((org.redisson.api.RBloomFilter) bloomFilter);
        when(bloomFilter.tryInit(anyLong(), anyDouble())).thenReturn(true);

        productBloomFilterComponent.add("P1");

        verify(bloomFilter).tryInit(Constants.PRODUCT_BLOOM_EXPECTED_INSERTIONS,
                Constants.PRODUCT_BLOOM_FALSE_PROBABILITY);
        verify(bloomFilter).add("P1");
    }

    @Test
    void add_swallowsFailure() {
        when(redissonClient.getBloomFilter(anyString())).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> productBloomFilterComponent.add("P1"));
    }

    @Test
    void mightExist_emptyProductId_returnsFalse() {
        assertFalse(productBloomFilterComponent.mightExist(""));
        assertFalse(productBloomFilterComponent.mightExist(null));
        verifyNoInteractions(redissonClient);
    }

    @Test
    void mightExist_notReady_returnsTrue() {
        assertTrue(productBloomFilterComponent.mightExist("P1"));
        verifyNoInteractions(redissonClient);
    }

    @Test
    void warmUpAllProductsOnStartup_populatesFilter() {
        when(redissonClient.getBloomFilter(Constants.REDIS_KEY_PRODUCT_BLOOM)).thenReturn((org.redisson.api.RBloomFilter) bloomFilter);
        when(bloomFilter.tryInit(anyLong(), anyDouble())).thenReturn(true);
        when(productInfoMapper.selectAllProductIds()).thenReturn(List.of("P1", "P2"));

        productBloomFilterComponent.warmUpAllProductsOnStartup();

        verify(bloomFilter).add("P1");
        verify(bloomFilter).add("P2");
        // 幂等：第二次预热直接跳过
        productBloomFilterComponent.warmUpAllProductsOnStartup();
        verify(productInfoMapper, times(1)).selectAllProductIds();
    }
}
