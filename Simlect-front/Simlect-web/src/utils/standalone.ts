
export const isStandaloneDisplay = () => {
  if (typeof window === 'undefined') return false;
  return (
    window.matchMedia('(display-mode: standalone)').matches ||
    
    (window.navigator as Navigator & { standalone?: boolean }).standalone === true
  );
};
