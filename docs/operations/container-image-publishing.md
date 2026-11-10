
# TITAN Container Image Publishing and Retrieval

## 1. Purpose

This document defines the container image naming, tagging, publishing, retrieval, and identification strategy for TITAN Commerce.

The current implementation uses GitHub Container Registry (GHCR).

Amazon Elastic Container Registry (ECR) requirements are documented for a future AWS migration. No AWS resources are provisioned as part of this milestone.

## 2. Image Naming Convention

TITAN publishes separate images for its backend and frontend.

| Component | Image name |
|---|---|
| Backend | `ghcr.io/mohdshakerali/titan-commerce-backend` |
| Frontend | `ghcr.io/mohdshakerali/titan-commerce-frontend` |

The registry namespace identifies the GitHub account that owns the packages.

## 3. Image Tagging Strategy

Two tags are currently used for each image.

| Tag | Purpose |
|---|---|
| Git short SHA | Associates an image with a source-code revision |
| `latest` | Convenient reference to the most recently published image |

Example:

```text
ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84
ghcr.io/mohdshakerali/titan-commerce-backend:latest

ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
ghcr.io/mohdshakerali/titan-commerce-frontend:latest
```

### Mutable tags

Tags such as `latest` can be updated to reference a different image when a new image is published.

A Git SHA tag provides traceability to a source revision, but it is not automatically immutable. Registry permissions or tag-protection policies are needed to prevent reassignment.

### Immutable image identity

An image digest is a content-based identifier.

The digest is the preferred reference for deployments that require a specific image version.

Example:

```text
ghcr.io/mohdshakerali/titan-commerce-backend@sha256:<digest>
```

A digest avoids relying on a mutable tag to identify the image.

## 4. Image Build and Tagging

Images are built from the repository's existing Dockerfiles.

```powershell
docker compose build backend frontend
```

The current source revision was:

`48ebf84`

The images were tagged using:

```powershell
docker tag titan-commerce-backend:latest ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84

docker tag titan-commerce-backend:latest ghcr.io/mohdshakerali/titan-commerce-backend:latest

docker tag titan-commerce-frontend:latest ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84

docker tag titan-commerce-frontend:latest ghcr.io/mohdshakerali/titan-commerce-frontend:latest
```

The two tags for each component initially referred to the same local image content.

## 5. Registry Authentication and Credential Security

GHCR authentication was performed using a GitHub Personal Access Token (classic).

The token was granted the `write:packages` scope.

Authentication command:

```powershell
docker login ghcr.io -u mohdshakerali
```

The token was entered through Docker's interactive password prompt.

Security requirements:

- Never commit registry tokens to Git.
- Never store PAT values in Dockerfiles or Compose files.
- Never expose credentials in scripts, logs, or documentation.
- Use an appropriate credential store for local Docker authentication.
- For automated publishing, use protected CI secrets or an appropriate short-lived authentication mechanism.
- Grant only the permissions required for the operation.

The token value is deliberately not recorded in this document.

## 6. Publishing Images to GHCR

The following commands publish the version-specific images:

```powershell
docker push ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84

docker push ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

The convenience tags were also published:

```powershell
docker push ghcr.io/mohdshakerali/titan-commerce-backend:latest

docker push ghcr.io/mohdshakerali/titan-commerce-frontend:latest
```

The registry returned matching digests for the version-specific and latest tags.

This established that both tags referred to the same image index for each component.

## 7. Published Image Evidence

The following image index digests were returned by GHCR.

### Backend

Repository:

`ghcr.io/mohdshakerali/titan-commerce-backend`

Version tag:

`48ebf84`

Image index digest:

```text
sha256:7e1cf4e082012f745170f58357971c338130b050cc83f75e19959f701ea7b01d
```

### Frontend

Repository:

`ghcr.io/mohdshakerali/titan-commerce-frontend`

Version tag:

`48ebf84`

Image index digest:

```text
sha256:9cd42b6521b7cfe3b4b2d6e663f01f5bd4ae2ced679475fe82922b01de37e3b7
```

### OCI manifest information

Both images were published as OCI image indexes.

The inspected indexes contained:

- A `linux/amd64` application image manifest.
- An additional provenance attestation manifest marked with the `unknown/unknown` platform.

The index digest identifies the complete OCI index. Individual platform manifests have their own digests.

## 8. Image Retrieval and Verification

The GHCR-qualified local references were removed:

```powershell
docker image rm ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84

docker image rm ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

The version-specific images were then retrieved from GHCR:

```powershell
docker pull ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84

docker pull ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

Both pull operations completed successfully.

The image indexes were verified using:

```powershell
docker buildx imagetools inspect ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84

docker buildx imagetools inspect ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

The returned digests matched the digests recorded during publishing.

Note: Original local image references still existed, so Docker could reuse locally cached layers. The test verified GHCR retrieval and manifest identity; it was not a guaranteed cold pull of every layer.

## 9. Referencing a Specific Image During Deployment

For a registry-based deployment, images can be referenced directly by digest.

Example:

```yaml
services:
  backend:
    image: ghcr.io/mohdshakerali/titan-commerce-backend@sha256:7e1cf4e082012f745170f58357971c338130b050cc83f75e19959f701ea7b01d

  frontend:
    image: ghcr.io/mohdshakerali/titan-commerce-frontend@sha256:9cd42b6521b7cfe3b4b2d6e663f01f5bd4ae2ced679475fe82922b01de37e3b7
```

This illustrates digest-pinned image references.

It is an example for a registry-based deployment, not a replacement for the current local development Compose configuration.

The current local development configuration continues to build images from the repository's Dockerfiles.

## 10. Amazon ECR — Future AWS Integration

Amazon Elastic Container Registry will be considered during the later AWS migration milestone.

The expected workflow is:

1. Create private ECR repositories for the backend and frontend.
2. Build the application images.
3. Authenticate the Docker client to the ECR registry.
4. Tag images with the ECR repository address.
5. Push images to ECR.
6. Retrieve images using their tags or digests.
7. Reference approved image digests in deployment configurations.

Example ECR authentication:

```powershell
aws ecr get-login-password --region <region> |
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
```

This command requires valid AWS credentials and appropriate IAM permissions. It has not been executed as part of M2-09.

### ECR IAM concepts

The future implementation should separate publishing permissions from runtime retrieval permissions.

**Image publisher role**

The CI/CD publishing identity needs permissions such as:

- `ecr:GetAuthorizationToken`
- `ecr:BatchCheckLayerAvailability`
- `ecr:InitiateLayerUpload`
- `ecr:UploadLayerPart`
- `ecr:CompleteLayerUpload`
- `ecr:PutImage`

The AWS ECR push policy documentation also includes `ecr:BatchGetImage`.

Repository content permissions should be scoped to the required ECR repository ARN. `ecr:GetAuthorizationToken` requires resource `*`.

**Image puller role**

The deployment identity or ECS task execution role needs:

- `ecr:GetAuthorizationToken`
- `ecr:BatchGetImage`
- `ecr:GetDownloadUrlForLayer`

These permissions allow a deployment environment to authenticate and retrieve the image.

The principle of least privilege should be followed. Runtime workloads should not receive image publishing permissions.

No ECR repository, IAM role, or paid AWS resource was created during this milestone.

## 11. Cost and Registry Considerations

GHCR was selected for this local publishing exercise.

GitHub's current billing documentation states that container image storage and bandwidth for GitHub Container Registry are free. This policy should be checked periodically.

For private GitHub packages, confirm package access settings and visibility. Newly published packages under personal accounts default to private visibility.

No paid cloud registry was provisioned.

## 12. M2-09 Validation Evidence

The following activities were successfully completed:

- Backend and frontend images built locally.
- Consistent GHCR image names established.
- Version-specific tags created using Git revision `48ebf84`.
- `latest` tags created.
- GHCR authentication completed successfully.
- Backend image published successfully.
- Frontend image published successfully after retrying a failed publication attempt.
- Both `latest` tags published successfully.
- Backend image retrieved from GHCR.
- Frontend image retrieved from GHCR.
- Both OCI image indexes inspected successfully.
- Published digests matched the corresponding `latest` tag digests.
- Registry credentials were not included in repository files.

The initial frontend push returned a registry denial after layer transfer. Retrying the push succeeded without rebuilding the image. The exact cause of the initial rejection was not established.

## 13. Operational Conclusion

TITAN now has a verified local-to-registry image workflow.

The project demonstrates:

- Consistent container image naming.
- Source-revision-based tagging.
- Mutable tags versus immutable digest identity.
- GHCR authentication and credential handling.
- Container image publishing.
- Container image retrieval.
- OCI index and manifest inspection.
- Future Amazon ECR publishing and IAM requirements.

The current GHCR workflow provides the registry foundation for future automated image publishing and deployment integration.
