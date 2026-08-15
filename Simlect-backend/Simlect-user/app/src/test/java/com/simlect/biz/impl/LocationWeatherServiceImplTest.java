package com.simlect.biz.impl;

import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.dto.UserLocationCoordsDTO;
import com.simlect.entity.vo.LocationWeatherVO;
import com.simlect.exception.BusinessException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationWeatherServiceImplTest {

    @Mock
    private AppConfig appConfig;
    @Mock
    private OkHttpClient okHttpClient;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private LocationWeatherServiceImpl locationWeatherService;

    @Test
    void resolve_nullCoords_throws() {
        assertThrows(BusinessException.class, () -> locationWeatherService.resolve(null, 113.0));
        assertThrows(BusinessException.class, () -> locationWeatherService.resolve(23.0, null));
    }

    @Test
    void resolve_noApiKey_returnsLatLngOnly() {
        when(appConfig.getAmapKey()).thenReturn(null);

        LocationWeatherVO vo = locationWeatherService.resolve(23.0, 113.0);

        assertEquals(23.0, vo.getLatitude());
        assertEquals(113.0, vo.getLongitude());
        assertEquals("当地", vo.getSummary());
        verifyNoInteractions(okHttpClient);
    }

    @Test
    void resolve_withAmapSuccess_parsesAddressComponent() throws IOException {
        when(appConfig.getAmapKey()).thenReturn("amap-key");
        Call call = mock(Call.class);
        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        Response response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        String body = "{\"status\":\"1\",\"regeocode\":{\"addressComponent\":{"
                + "\"province\":\"广东省\",\"city\":\"广州市\",\"district\":\"天河区\","
                + "\"streetNumber\":{\"number\":\"88号\"}}}}";
        when(response.body()).thenReturn(ResponseBody.create(
                okhttp3.MediaType.parse("application/json"), body));
        when(call.execute()).thenReturn(response);

        LocationWeatherVO vo = locationWeatherService.resolve(23.0, 113.0);

        assertEquals("广东省", vo.getProvince());
        assertEquals("广州市", vo.getCity());
        assertEquals("天河区", vo.getDistrict());
        assertEquals("广州", vo.getSummary());
    }

    @Test
    void resolve_amapBusinessFailure_returnsGracefully() throws IOException {
        when(appConfig.getAmapKey()).thenReturn("amap-key");
        Call call = mock(Call.class);
        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        Response response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(ResponseBody.create(
                okhttp3.MediaType.parse("application/json"),
                "{\"status\":\"0\",\"info\":\"INVALID_USER_KEY\"}"));
        when(call.execute()).thenReturn(response);

        LocationWeatherVO vo = locationWeatherService.resolve(23.0, 113.0);

        assertNull(vo.getCity());
        assertEquals("当地", vo.getSummary());
    }

    @Test
    void saveUserCoords_persistsToRedis() {
        LocationWeatherVO resolved = new LocationWeatherVO();
        resolved.setCity("广州市");

        locationWeatherService.saveUserCoords("U1", 23.0, 113.0, resolved);

        verify(redisComponent).saveUserLocationCoords(eq("U1"), any(UserLocationCoordsDTO.class));
    }

    @Test
    void syncUserLocation_savesResolvedCoords() {
        when(appConfig.getAmapKey()).thenReturn(null);

        LocationWeatherVO vo = locationWeatherService.syncUserLocation("U1", 23.0, 113.0);

        assertNotNull(vo);
        verify(redisComponent).saveUserLocationCoords(eq("U1"), any(UserLocationCoordsDTO.class));
    }
}
