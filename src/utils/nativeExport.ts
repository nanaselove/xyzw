import { getAndroidBridge, isAndroidWebView } from "./env";

const EXCEL_MIME_TYPE =
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
const PNG_MIME_TYPE = "image/png";

export type SaveInput = Blob | HTMLCanvasElement;

const hasDocument = () => typeof document !== "undefined";

const sanitizeBase64 = (base64: string) => {
  const trimmed = base64.trim();
  const commaIndex = trimmed.indexOf(",");
  return commaIndex >= 0 ? trimmed.slice(commaIndex + 1) : trimmed;
};

const base64ToBlob = (base64: string, mimeType: string) => {
  const cleanBase64 = sanitizeBase64(base64);
  const binaryString = atob(cleanBase64);
  const bytes = new Uint8Array(binaryString.length);

  for (let index = 0; index < binaryString.length; index += 1) {
    bytes[index] = binaryString.charCodeAt(index);
  }

  return new Blob([bytes], { type: mimeType });
};

const arrayBufferToBase64 = (buffer: ArrayBuffer) => {
  const bytes = new Uint8Array(buffer);
  let binary = "";

  for (let index = 0; index < bytes.length; index += 0x2000) {
    binary += String.fromCharCode(...bytes.subarray(index, index + 0x2000));
  }

  return btoa(binary);
};

const blobToBase64 = async (blob: Blob) => {
  const buffer = await blob.arrayBuffer();
  return arrayBufferToBase64(buffer);
};

const canvasToBlob = (canvas: HTMLCanvasElement, mimeType: string) =>
  new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error("Canvas toBlob failed"));
          return;
        }

        resolve(blob);
      },
      mimeType,
    );
  });

const normalizeToBlob = async (input: SaveInput, mimeType: string) => {
  if (
    typeof globalThis.HTMLCanvasElement !== "undefined" &&
    input instanceof globalThis.HTMLCanvasElement
  ) {
    return canvasToBlob(input, mimeType);
  }

  if (input.type) {
    return input;
  }

  return new Blob([input], { type: mimeType });
};

const downloadBlob = (blob: Blob, fileName: string) => {
  if (!hasDocument()) {
    throw new Error("Document is not available");
  }

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = fileName;
  link.rel = "noopener";

  document.body.appendChild(link);
  link.click();
  link.remove();

  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
};

export const saveBase64File = async (
  base64: string,
  fileName: string,
  mimeType: string,
) => {
  const cleanBase64 = sanitizeBase64(base64);
  const bridge = getAndroidBridge();

  if (bridge?.saveBase64File && isAndroidWebView()) {
    bridge.saveBase64File(cleanBase64, fileName, mimeType);
    return;
  }

  downloadBlob(base64ToBlob(cleanBase64, mimeType), fileName);
};

export const saveBlob = async (
  input: SaveInput,
  fileName: string,
  mimeType = "application/octet-stream",
) => {
  const blob = await normalizeToBlob(input, mimeType);

  if (isAndroidWebView()) {
    await saveBase64File(
      await blobToBase64(blob),
      fileName,
      blob.type || mimeType,
    );
    return;
  }

  downloadBlob(blob, fileName);
};

export const saveExcel = async (input: SaveInput, fileName: string) => {
  await saveBlob(input, fileName, EXCEL_MIME_TYPE);
};

export const savePng = async (input: SaveInput, fileName: string) => {
  await saveBlob(input, fileName, PNG_MIME_TYPE);
};
