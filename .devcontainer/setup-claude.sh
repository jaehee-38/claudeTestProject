#!/usr/bin/env bash
# Claude Code 설정/로그인 정보를 rebuild 후에도 유지하기 위한 스크립트
# ~/.claude, ~/.claude.json 을 /workspaces(영구 저장 영역)로 옮기고 심볼릭 링크로 연결합니다.

PERSIST_DIR="/workspaces/.claude-home"
mkdir -p "$PERSIST_DIR"

link_persist() {
  local src="$1" dest="$2"
  # 이미 링크되어 있으면 건너뜀
  [ -L "$src" ] && return
  # 기존 파일이 있고 영구 저장소에 아직 없으면 옮김 (최초 1회)
  if [ -e "$src" ] && [ ! -e "$dest" ]; then
    mv "$src" "$dest"
  else
    rm -rf "$src"
  fi
  ln -s "$dest" "$src"
}

mkdir -p "$PERSIST_DIR/.claude"
link_persist "$HOME/.claude" "$PERSIST_DIR/.claude"
[ -e "$PERSIST_DIR/.claude.json" ] || [ -e "$HOME/.claude.json" ] || echo '{}' > "$PERSIST_DIR/.claude.json"
link_persist "$HOME/.claude.json" "$PERSIST_DIR/.claude.json"

echo "Claude config linked to $PERSIST_DIR"
