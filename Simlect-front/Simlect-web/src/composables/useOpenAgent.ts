import { useRouter } from 'vue-router';
import { useDevice } from '@/composables/useDevice';
import { agentApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';
import {
  saveAgentConsultProduct,
  type AgentConsultProduct
} from '@/utils/agentProductConsult';

export function useOpenAgent() {
  const router = useRouter();
  const authStore = useAuthStore();
  const { isDesktop } = useDevice();
  const pcAgentPanel = usePcAgentPanelStore();

  const pauseProductConsultContext = () => {
    if (!authStore.isLoggedIn) return;
    void agentApi.pauseProductConsult().catch(() => {});
  };

  const buildRedirect = (fromProduct?: boolean, presetMessage?: string) => {
    const params = new URLSearchParams();
    if (fromProduct) params.set('fromProduct', '1');
    if (presetMessage) params.set('msg', presetMessage);
    const q = params.toString();
    return `/ai-assistant${q ? '?' + q : ''}`;
  };

  const openAgent = (options?: { consultProduct?: AgentConsultProduct | null; fromProduct?: boolean; presetMessage?: string }) => {
    const fromProduct = !!options?.fromProduct || !!options?.consultProduct?.productId;
    const redirect = buildRedirect(fromProduct, options?.presetMessage);
    const userId = authStore.userInfo?.userId as string | undefined;

    if (options?.consultProduct?.productId) {
      saveAgentConsultProduct(options.consultProduct, userId);
    } else if (!fromProduct) {
      pauseProductConsultContext();
    }

    if (!authStore.isLoggedIn) {
      router.push({ path: '/login', query: { redirect } });
      return;
    }

    if (isDesktop.value) {
      pcAgentPanel.open({ fromProduct });
      return;
    }

    const query: Record<string, string> = {};
    if (fromProduct) query.fromProduct = '1';
    if (options?.presetMessage) query.msg = options.presetMessage;

    router.push({
      path: '/ai-assistant',
      query: Object.keys(query).length ? query : undefined
    });
  };

  return { openAgent };
}
