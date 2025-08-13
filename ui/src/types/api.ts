export const ApiErrorType = {
  USER_ALREADY_EXISTS: "USER_ALREADY_EXISTS",
  PASSWORD_TOO_WEAK: "PASSWORD_TOO_WEAK",
} as const;

export type ApiErrorType = (typeof ApiErrorType)[keyof typeof ApiErrorType];

export interface ErrorResponse {
  message: string;
  errorType: ApiErrorType;
}

export interface ApiError {
  response?: {
    data: ErrorResponse;
  };
}

export function isApiError(error: unknown): error is ApiError & { response: { data: ErrorResponse } } {
  return (
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    typeof (error as any).response === "object" &&
    (error as any).response !== null &&
    "data" in (error as any).response &&
    typeof (error as any).response.data === "object" &&
    (error as any).response.data !== null &&
    "errorType" in (error as any).response.data
  );
}
