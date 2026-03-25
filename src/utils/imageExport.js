import html2canvas from "html2canvas";

/**
 * Download a canvas as an image.
 * Keeps the existing share-first behavior on supported mobile browsers.
 */
export const downloadCanvasAsImage = (canvas, filename) => {
  try {
    if (canvas.toBlob) {
      canvas.toBlob((blob) => {
        if (!blob) {
          console.error("Canvas to Blob failed");
          fallbackToDataURL(canvas, filename);
          return;
        }

        if (
          navigator.share &&
          navigator.canShare &&
          navigator.canShare({ files: [new File([blob], filename, { type: blob.type })] })
        ) {
          const file = new File([blob], filename, { type: blob.type });
          navigator
            .share({
              files: [file],
              title: "Share image",
              text: filename,
            })
            .catch((err) => {
              console.log("Share failed, falling back to download", err);
              downloadBlob(blob, filename);
            });
        } else {
          downloadBlob(blob, filename);
        }
      }, "image/png");
    } else {
      fallbackToDataURL(canvas, filename);
    }
  } catch (e) {
    console.error("Image export failed:", e);
    fallbackToDataURL(canvas, filename);
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
    throw new Error("未找到要导出的DOM元素");
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

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;

  document.body.appendChild(link);

  try {
    link.click();
  } catch (e) {
    console.error("Link click failed", e);
  }

  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(url), 100);
};

const fallbackToDataURL = (canvas, filename) => {
  try {
    const imgUrl = canvas.toDataURL("image/png");
    const link = document.createElement("a");
    link.href = imgUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (e) {
    console.error("DataURL export failed:", e);
    alert("导出图片失败，图片可能过大");
  }
};
