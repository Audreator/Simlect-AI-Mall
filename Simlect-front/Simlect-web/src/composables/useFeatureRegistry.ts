import {
  FEATURE_REGISTRY,
  isFeatureSupported,
  unsupportedFeatures,
  type FeatureId
} from '@/integrations/featureRegistry';


export function useFeatureRegistry() {
  return {
    FEATURE_REGISTRY,
    isFeatureSupported,
    unsupportedFeatures
  };
}

export type { FeatureId };
