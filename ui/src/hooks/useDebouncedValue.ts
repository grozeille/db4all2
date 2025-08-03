import { useEffect, useState } from 'react';

/**
 * useDebouncedValue
 * Retourne la valeur après un délai d'inactivité (debounce).
 * @param value La valeur à surveiller
 * @param delay Le délai en ms
 */
export function useDebouncedValue<T>(value: T, delay: number = 2000): T {
  const [debounced, setDebounced] = useState<T>(value);
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebounced(value);
    }, delay);
    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);
  return debounced;
}
