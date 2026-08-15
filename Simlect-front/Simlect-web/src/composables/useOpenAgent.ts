// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/api/modules`
import { agentApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/stores/pcAgentPanel`
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`saveAgentConsultProduct,`
  saveAgentConsultProduct,
  // [zh] TypeScript 类型 `AgentConsultProduct`（类比 Java interface/DTO）
  type AgentConsultProduct
// [zh] 语句
} from '@/utils/agentProductConsult';

// [zh] 导出模块成员
export function useOpenAgent() {
  // [zh] 声明变量 `router`
  const router = useRouter();
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 语句
  const { isDesktop } = useDevice();
  // [zh] 声明变量 `pcAgentPanel`
  const pcAgentPanel = usePcAgentPanelStore();

  // [zh] 声明变量 `pauseProductConsultContext`
  const pauseProductConsultContext = () => {
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) return;
    // [zh] 语句
    void agentApi.pauseProductConsult().catch(() => {});
  // [zh] 语句
  };

  // [zh] 声明变量 `buildRedirect`
  const buildRedirect = (fromProduct?: boolean, presetMessage?: string) => {
    // [zh] 声明变量 `params`
    const params = new URLSearchParams();
    // [zh] 条件判断 if
    if (fromProduct) params.set('fromProduct', '1');
    // [zh] 条件判断 if
    if (presetMessage) params.set('msg', presetMessage);
    // [zh] 声明变量 `q`
    const q = params.toString();
    // [zh] return 返回
    return `/ai-assistant${q ? '?' + q : ''}`;
  // [zh] 语句
  };

  // [zh] 声明变量 `openAgent`
  const openAgent = (options?: { consultProduct?: AgentConsultProduct | null; fromProduct?: boolean; presetMessage?: string }) => {
    // [zh] 声明变量 `fromProduct`
    const fromProduct = !!options?.fromProduct || !!options?.consultProduct?.productId;
    // [zh] 声明变量 `redirect`
    const redirect = buildRedirect(fromProduct, options?.presetMessage);
    // [zh] 声明变量 `userId`
    const userId = authStore.userInfo?.userId as string | undefined;

    // [zh] 条件判断 if
    if (options?.consultProduct?.productId) {
      // [zh] 语句
      saveAgentConsultProduct(options.consultProduct, userId);
    // [zh] 代码块开始
    } else if (!fromProduct) {
      // [zh] 语句
      pauseProductConsultContext();
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) {
      // [zh] 语句
      router.push({ path: '/login', query: { redirect } });
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (isDesktop.value) {
      // [zh] 语句
      pcAgentPanel.open({ fromProduct });
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `query`
    const query: Record<string, string> = {};
    // [zh] 条件判断 if
    if (fromProduct) query.fromProduct = '1';
    // [zh] 条件判断 if
    if (options?.presetMessage) query.msg = options.presetMessage;

    // [zh] 代码块开始
    router.push({
      // [zh] TypeScript/JS 语句：`path: '/ai-assistant',`
      path: '/ai-assistant',
      // [zh] TypeScript/JS 语句：`query: Object.keys(query).length ? query : undefined`
      query: Object.keys(query).length ? query : undefined
    // [zh] 语句
    });
  // [zh] 语句
  };

  // [zh] return 返回
  return { openAgent };
// [zh] 语法符号
}
