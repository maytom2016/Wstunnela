#!/bin/bash

set -e

# 设置目标目录
TARGET_DIR="app/src/main/assets"
mkdir -p "$TARGET_DIR"

# 获取最新 release 的所有信息
RELEASE_JSON=$(curl -s https://api.github.com/repos/erebe/wstunnel/releases/latest)

# 提取 tag（比如 v10.5.0）
LATEST_TAG=$(echo "$RELEASE_JSON" | jq -r '.tag_name')
echo "最新版本: $LATEST_TAG"

# 架构与目标文件名映射
declare -A ARCH_MAP=(
  ["linux_amd64"]="wstunnel_amd64"
  ["android_arm64"]="wstunnel_arm64"
)

# 遍历架构并处理
for ARCH in "${!ARCH_MAP[@]}"; do
  # 查找下载链接
  DOWNLOAD_URL=$(echo "$RELEASE_JSON" | jq -r \
    --arg arch "$ARCH" \
    '.assets[] | select(.name | test($arch)) | .browser_download_url')

  if [ -z "$DOWNLOAD_URL" ]; then
    echo "❌ 未找到 $ARCH 对应的下载链接"
    continue
  fi

  echo "✅ 找到 $ARCH 下载链接: $DOWNLOAD_URL"

  # 下载并解压
  TEMP_DIR=$(mktemp -d)
  curl -L "$DOWNLOAD_URL" -o "$TEMP_DIR/archive.tar.gz"
  tar -xzf "$TEMP_DIR/archive.tar.gz" -C "$TEMP_DIR"

  # 查找二进制文件
  BIN_PATH=$(find "$TEMP_DIR" -type f -name "wstunnel")
  if [ -z "$BIN_PATH" ]; then
    echo "❌ 未找到 wstunnel 可执行文件"
    exit 1
  fi

  # 保存并设置权限
  cp "$BIN_PATH" "$TARGET_DIR/${ARCH_MAP[$ARCH]}"
  chmod +x "$TARGET_DIR/${ARCH_MAP[$ARCH]}"
  echo "✅ 已保存至 $TARGET_DIR/${ARCH_MAP[$ARCH]}"

  # 清理临时目录
  rm -rf "$TEMP_DIR"
done

echo "✅ 所有架構已完成更新"
