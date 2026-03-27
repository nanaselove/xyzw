export {};

declare global {
  interface AndroidBridge {
    saveBase64File(base64: string, fileName: string, mimeType: string): void;
    shareBase64File?(base64: string, fileName: string, mimeType: string): void;
    shareText?(text: string, title: string): void;
    openExternalUrl?(url: string): void;
    checkApkUpdate?(): void;
    requestText?(
      requestId: string,
      url: string,
      method: string,
      headersJson: string,
      body: string,
      timeoutMs: number,
    ): void;
  }

  interface Window {
    AndroidBridge?: AndroidBridge;
  }
}
