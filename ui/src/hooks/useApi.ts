import { useState } from 'react';

// The API function can have any number of arguments of any type.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type ApiFunc<T> = (...args: any[]) => Promise<T>;

interface UseApiReturn<T> {
  loading: boolean;
  error: string | null;
  data: T | null;
  // The request function will have the same arguments as the original API function.
  request: ApiFunc<T>;
}

export const useApi = <T>(apiFunc: ApiFunc<T>): UseApiReturn<T> => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<T | null>(null);

  const request = async (...args: Parameters<ApiFunc<T>>): Promise<T> => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiFunc(...args);
      setData(result);
      setLoading(false);
      return result;
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'An unknown error occurred';
      setError(errorMessage);
      setLoading(false);
      // Re-throw the error so that the caller can handle it if needed
      throw err;
    }
  };

  return { loading, error, data, request };
};
