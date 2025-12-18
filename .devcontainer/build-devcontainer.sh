#!/usr/bin/env bash
# build-devcontainer.sh
# 
# Build and push the Aether Android Development container image
# Always uses 'latest' tag for devcontainer simplicity
#
# Usage:
#   ./build-devcontainer.sh [REGISTRY]
#
# Arguments:
#   REGISTRY  - Registry to push to (optional)
#               Examples:
#                 ghcr.io/username        (GitHub Container Registry)
#                 docker.io/username      (Docker Hub)
#
# Examples:
#   # Build locally only
#   ./build-devcontainer.sh
#
#   # Build and push to GitHub Container Registry
#   ./build-devcontainer.sh ghcr.io/yourusername
#
# Note: For GitHub Container Registry, authenticate first:
#   echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
#   or: gh auth token | docker login ghcr.io -u USERNAME --password-stdin
#
# For local development with Rancher Desktop:
#   - Build without registry argument
#   - Image is available immediately to devcontainer
#   - No registry setup or push needed

set -e  # Exit on error

# Configuration
IMAGE_NAME="aether-android-dev"
DOCKERFILE_PATH=".devcontainer/dockerfile"
BUILD_CONTEXT="."
PLATFORM="linux/amd64"
TAG="latest"  # Always use latest for devcontainers

# Parse arguments
REGISTRY="${1:-}"

# Construct image names
LOCAL_IMAGE="${IMAGE_NAME}:${TAG}"

if [ -n "$REGISTRY" ]; then
    REMOTE_IMAGE="${REGISTRY}/${IMAGE_NAME}:${TAG}"
else
    REMOTE_IMAGE=""
fi

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Aether Android Development Container Builder${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo -e "  Image Name:    ${LOCAL_IMAGE}"
echo -e "  Platform:      ${PLATFORM}"
echo -e "  Dockerfile:    ${DOCKERFILE_PATH}"
echo -e "  Build Context: ${BUILD_CONTEXT}"
if [ -n "$REMOTE_IMAGE" ]; then
    echo -e "  Push To:       ${REMOTE_IMAGE}"
else
    echo -e "  Push To:       ${YELLOW}(local only)${NC}"
fi
echo ""

# Check if Dockerfile exists
if [ ! -f "$DOCKERFILE_PATH" ]; then
    echo -e "${YELLOW}Error: Dockerfile not found at ${DOCKERFILE_PATH}${NC}"
    exit 1
fi

# Build the image
echo -e "${GREEN}Step 1: Building Docker image...${NC}"
docker --debug build \
    --platform="${PLATFORM}" \
    -t "${LOCAL_IMAGE}" \
    -f "${DOCKERFILE_PATH}" \
    "${BUILD_CONTEXT}"

echo ""
echo -e "${GREEN}✓ Build complete: ${LOCAL_IMAGE}${NC}"
echo ""

# Tag and push if registry specified
if [ -n "$REMOTE_IMAGE" ]; then
    echo -e "${GREEN}Step 2: Tagging image for registry...${NC}"
    docker tag "${LOCAL_IMAGE}" "${REMOTE_IMAGE}"
    echo -e "${GREEN}✓ Tagged: ${REMOTE_IMAGE}${NC}"
    echo ""
    
    echo -e "${GREEN}Step 3: Pushing to registry...${NC}"
    docker push "${REMOTE_IMAGE}"
    echo ""
    echo -e "${GREEN}✓ Push complete: ${REMOTE_IMAGE}${NC}"
    echo ""
    echo -e "${BLUE}To use in devcontainer.json:${NC}"
    echo -e '  "image": "'${REMOTE_IMAGE}'"'
else
    echo -e "${BLUE}To use in devcontainer.json:${NC}"
    echo -e '  "image": "'${LOCAL_IMAGE}'"'
fi

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Done!${NC}"
echo -e "${GREEN}================================================${NC}"