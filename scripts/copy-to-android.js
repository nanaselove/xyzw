import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, "..");
const distDir = path.resolve(rootDir, "dist");
const androidAssetsDir = path.resolve(
  rootDir,
  "android-app/app/src/main/assets/www",
);
const androidTempAssetsDir = path.resolve(
  os.tmpdir(),
  "xyzw-web-helper-android-assets/www",
);

const copyEntry = (sourcePath, destinationPath) => {
  const sourceStat = fs.lstatSync(sourcePath);

  if (sourceStat.isSymbolicLink()) {
    const resolvedSourcePath = fs.realpathSync(sourcePath);
    return copyEntry(resolvedSourcePath, destinationPath);
  }

  if (sourceStat.isDirectory()) {
    fs.mkdirSync(destinationPath, { recursive: true });
    for (const entry of fs.readdirSync(sourcePath)) {
      copyEntry(
        path.join(sourcePath, entry),
        path.join(destinationPath, entry),
      );
    }
    return;
  }

  if (sourceStat.isFile()) {
    fs.mkdirSync(path.dirname(destinationPath), { recursive: true });
    const fileBuffer = fs.readFileSync(sourcePath);
    const tempDestinationPath = `${destinationPath}.tmp-${process.pid}-${Date.now()}`;
    fs.writeFileSync(tempDestinationPath, fileBuffer);
    fs.rmSync(destinationPath, { force: true });
    fs.renameSync(tempDestinationPath, destinationPath);
    return;
  }

  throw new Error(`Unsupported asset type at ${sourcePath}`);
};

const normalizeFiles = (rootPath) => {
  const entries = fs.readdirSync(rootPath, { withFileTypes: true });

  for (const entry of entries) {
    const entryPath = path.join(rootPath, entry.name);

    if (entry.isDirectory()) {
      normalizeFiles(entryPath);
      continue;
    }

    const stat = fs.lstatSync(entryPath);
    if (!stat.isFile() && !stat.isSymbolicLink()) {
      continue;
    }

    const fileBuffer = fs.readFileSync(entryPath);
    const tempPath = `${entryPath}.tmp-${process.pid}-${Date.now()}`;
    fs.writeFileSync(tempPath, fileBuffer);
    fs.rmSync(entryPath, { force: true });
    fs.renameSync(tempPath, entryPath);
  }
};

if (!fs.existsSync(distDir)) {
  throw new Error(
    `Build output not found at ${distDir}. Run "npm run build" first.`,
  );
}

fs.rmSync(androidAssetsDir, { recursive: true, force: true });
fs.mkdirSync(path.dirname(androidAssetsDir), { recursive: true });
copyEntry(distDir, androidAssetsDir);
normalizeFiles(androidAssetsDir);

fs.rmSync(androidTempAssetsDir, { recursive: true, force: true });
fs.mkdirSync(path.dirname(androidTempAssetsDir), { recursive: true });
copyEntry(distDir, androidTempAssetsDir);
normalizeFiles(androidTempAssetsDir);

console.log(`Copied ${distDir} -> ${androidAssetsDir}`);
console.log(`Copied ${distDir} -> ${androidTempAssetsDir}`);
