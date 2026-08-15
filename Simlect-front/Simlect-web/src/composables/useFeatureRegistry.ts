// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`FEATURE_REGISTRY,`
  FEATURE_REGISTRY,
  // [zh] TypeScript/JS 语句：`isFeatureSupported,`
  isFeatureSupported,
  // [zh] TypeScript/JS 语句：`unsupportedFeatures,`
  unsupportedFeatures,
  // [zh] TypeScript 类型 `FeatureId`（类比 Java interface/DTO）
  type FeatureId
// [zh] 语句
} from '@/integrations/featureRegistry';


// [zh] 导出模块成员
export function useFeatureRegistry() {
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`FEATURE_REGISTRY,`
    FEATURE_REGISTRY,
    // [zh] TypeScript/JS 语句：`isFeatureSupported,`
    isFeatureSupported,
    // [zh] TypeScript/JS 语句：`unsupportedFeatures`
    unsupportedFeatures
  // [zh] 语句
  };
// [zh] 语法符号
}

// [zh] 导出模块成员
export type { FeatureId };
