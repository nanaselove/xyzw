import html2canvas from "html2canvas";
import { savePng } from "./nativeExport";

/**
 * Compatibility wrapper for image exports.
 * The actual save flow is centralized in nativeExport.ts.
 */
export const downloadCanvasAsImage = async (canvas, filename) => {
  try {
    await savePng(canvas, filename);
  } catch (error) {
    console.error("Image export failed:", error);
  }
};

const DEFAULT_MEASURE_SELECTORS = [
  ".n-data-table-base-table-header",
  ".n-data-table-base-table-body",
  ".n-scrollbar-container",
  ".n-scrollbar-content",
  ".n-data-table-base-table-body__content",
  ".n-data-table__table",
  "table",
];

const getBoxSize = (element) => {
  if (!element) {
    return { width: 0, height: 0 };
  }

  return {
    width: Math.max(
      element.scrollWidth || 0,
      element.clientWidth || 0,
      element.offsetWidth || 0,
    ),
    height: Math.max(
      element.scrollHeight || 0,
      element.clientHeight || 0,
      element.offsetHeight || 0,
    ),
  };
};

const collectBoxSize = (element, selectors = []) => {
  const base = getBoxSize(element);
  let width = base.width;
  let height = base.height;

  [...new Set([...DEFAULT_MEASURE_SELECTORS, ...selectors].filter(Boolean))].forEach((selector) => {
    element.querySelectorAll(selector).forEach((node) => {
      width = Math.max(width, node.scrollWidth || 0, node.clientWidth || 0, node.offsetWidth || 0);
      height = Math.max(
        height,
        node.scrollHeight || 0,
        node.clientHeight || 0,
        node.offsetHeight || 0,
      );
    });
  });

  return { width, height };
};

export const trimCanvasWhitespace = (canvas, padding = 16, threshold = 248) => {
  const ctx = canvas.getContext("2d");
  if (!ctx) return canvas;

  const { width, height } = canvas;
  const imageData = ctx.getImageData(0, 0, width, height);
  const { data } = imageData;

  let top = height;
  let left = width;
  let right = -1;
  let bottom = -1;

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const index = (y * width + x) * 4;
      const alpha = data[index + 3];
      if (!alpha) continue;

      const red = data[index];
      const green = data[index + 1];
      const blue = data[index + 2];
      if (red >= threshold && green >= threshold && blue >= threshold) {
        continue;
      }

      if (x < left) left = x;
      if (x > right) right = x;
      if (y < top) top = y;
      if (y > bottom) bottom = y;
    }
  }

  if (right < left || bottom < top) {
    return canvas;
  }

  const cropLeft = Math.max(0, left - padding);
  const cropTop = Math.max(0, top - padding);
  const cropRight = Math.min(width, right + padding + 1);
  const cropBottom = Math.min(height, bottom + padding + 1);
  const cropWidth = cropRight - cropLeft;
  const cropHeight = cropBottom - cropTop;

  if (cropWidth <= 0 || cropHeight <= 0) {
    return canvas;
  }

  if (cropWidth === width && cropHeight === height) {
    return canvas;
  }

  const trimmedCanvas = document.createElement("canvas");
  trimmedCanvas.width = cropWidth;
  trimmedCanvas.height = cropHeight;

  const trimmedCtx = trimmedCanvas.getContext("2d");
  if (!trimmedCtx) return canvas;

  trimmedCtx.drawImage(
    canvas,
    cropLeft,
    cropTop,
    cropWidth,
    cropHeight,
    0,
    0,
    cropWidth,
    cropHeight,
  );

  return trimmedCanvas;
};

const applyCloneDefaults = (clonedDoc, renderWidth) => {
  const scrollTargets = clonedDoc.querySelectorAll(
    ".n-data-table-base-table-header, .n-data-table-base-table-body, .n-scrollbar-container",
  );
  scrollTargets.forEach((el) => {
    if ("scrollLeft" in el) {
      el.scrollLeft = 0;
    }
    if ("scrollTop" in el) {
      el.scrollTop = 0;
    }
    el.style.overflow = "visible";
    el.style.overflowX = "visible";
    el.style.overflowY = "visible";
  });

  clonedDoc
    .querySelectorAll(".n-data-table, .n-data-table-base-table")
    .forEach((el) => {
      el.style.width = "max-content";
      el.style.minWidth = "max-content";
      el.style.maxWidth = "none";
      el.style.height = "auto";
      el.style.overflow = "visible";
    });

  clonedDoc
    .querySelectorAll(".n-data-table-base-table-body__content, .n-data-table__table, table")
    .forEach((el) => {
      el.style.width = "max-content";
      el.style.minWidth = "max-content";
    });

  clonedDoc.querySelectorAll(".n-scrollbar-content").forEach((el) => {
    el.style.width = "max-content";
    el.style.minWidth = "max-content";
    el.style.height = "auto";
    el.style.overflow = "visible";
  });

  [clonedDoc.documentElement, clonedDoc.body].forEach((target) => {
    if (!target) return;
    target.style.width = `${renderWidth}px`;
    target.style.maxWidth = `${renderWidth}px`;
    target.style.height = "auto";
    target.style.overflow = "visible";
    target.style.margin = "0";
  });
};

/**
 * Capture a DOM element into a canvas with table-friendly defaults.
 */
export const captureDomCanvas = async (element, options = {}) => {
  if (!element) {
    throw new Error("未找到要导出的 DOM 元素");
  }

  const {
    scale = 2,
    useCORS = true,
    backgroundColor = "#ffffff",
    logging = false,
    allowTaint = true,
    trimPadding = 16,
    trimThreshold = 248,
    width,
    height,
    windowWidth,
    windowHeight,
    ignoreElements,
    onclone,
    measureSelectors = [],
    ...html2canvasOptions
  } = options;

  const box = collectBoxSize(element, measureSelectors);
  const renderWidth = width ?? Math.max(box.width || 0, 1);
  const renderHeight = height ?? Math.max(box.height || 0, 1);
  const renderWindowWidth = windowWidth ?? renderWidth;
  const renderWindowHeight = windowHeight ?? renderHeight;

  const combinedIgnoreElements = (node) => {
    if (node?.classList?.contains?.("no-export")) {
      return true;
    }
    if (typeof ignoreElements === "function" && ignoreElements(node)) {
      return true;
    }
    return false;
  };

  const canvas = await html2canvas(element, {
    scale,
    useCORS,
    backgroundColor,
    logging,
    allowTaint,
    width: renderWidth,
    height: renderHeight,
    windowWidth: renderWindowWidth,
    windowHeight: renderWindowHeight,
    ignoreElements: combinedIgnoreElements,
    onclone: (clonedDoc) => {
      applyCloneDefaults(clonedDoc, renderWidth);
      if (typeof onclone === "function") {
        onclone(clonedDoc, {
          renderWidth,
          renderHeight,
        });
      }
    },
    ...html2canvasOptions,
  });

  return trimCanvasWhitespace(canvas, trimPadding, trimThreshold);
};
