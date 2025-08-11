import { useState, useCallback } from 'react';

// The API function can have any number of arguments of any type.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type ApiFunc<T> = (...args: any[]) => Promise<T>;

interface UseApiReturn<T> {
  isLoading: boolean;
  error: Error | null; // Changed to store the full error object
  data: T | null;
  // The execute function can be called with arguments for the initial apiFunc,
  // or it can be called with a new apiFunc as the first argument.
  execute: (...args: any[]) => Promise<T | undefined>; // Return undefined on failure
}

/**
 * A generic hook for handling API calls.
 * It can be initialized with a specific API function, or used with functions passed to `execute`.
 * @param apiFunc An optional API function to be bound to the hook instance.
 * @returns An object with `data`, `error`, `isLoading` state, and an `execute` function.
 */
export const useApi = <T>(apiFunc?: ApiFunc<T>): UseApiReturn<T> => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null); // Changed to store the full error object
  const [data, setData] = useState<T | null>(null);

  const execute = useCallback(
    async (...args: any[]): Promise<T | undefined> => {
      // If the hook was initialized with an apiFunc, use it.
      // Otherwise, the first argument to execute must be the function to call.
      const funcToExecute = apiFunc || args[0];
      const callArgs = apiFunc ? args : args.slice(1);

      if (typeof funcToExecute !== 'function') {
        const err = new Error('API function was not provided to useApi hook or its execute method.');
        setError(err);
        return undefined;
      }

      setIsLoading(true);
      setError(null);
      try {
        const result = await funcToExecute(...callArgs);
        setData(result);
        return result;
      } catch (err) {
        // Store the entire error object so the component can inspect it
        if (err instanceof Error) {
          setError(err);
        } else {
          // If for some reason a non-Error is thrown, wrap it
          setError(new Error(String(err)));
        }
        // DO NOT re-throw the error. This prevents the "Uncaught (in promise)" crash.
        // The component will handle the error by observing the `error` state.
        return undefined;
      } finally {
        setIsLoading(false);
      }
    },
    [apiFunc]
  );

  return { isLoading, error, data, execute };
};
