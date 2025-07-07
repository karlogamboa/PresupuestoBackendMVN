#!/bin/bash

# Create S3 bucket for Lambda deployments
BUCKET_NAME="presupuesto-lambda-deployments"
REGION="us-east-2"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Creating S3 bucket for Lambda deployments...${NC}"

# Check if bucket already exists
if aws s3api head-bucket --bucket "$BUCKET_NAME" --region "$REGION" 2>/dev/null; then
    echo -e "${YELLOW}Bucket $BUCKET_NAME already exists${NC}"
else
    echo -e "${YELLOW}Creating bucket $BUCKET_NAME...${NC}"
    
    # Create bucket
    aws s3api create-bucket \
        --bucket "$BUCKET_NAME" \
        --region "$REGION" \
        --create-bucket-configuration LocationConstraint="$REGION"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Bucket created successfully${NC}"
    else
        echo -e "${RED}Error: Failed to create bucket${NC}"
        exit 1
    fi
fi

# Enable versioning
echo -e "${YELLOW}Enabling versioning...${NC}"
aws s3api put-bucket-versioning \
    --bucket "$BUCKET_NAME" \
    --versioning-configuration Status=Enabled

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Versioning enabled successfully${NC}"
else
    echo -e "${RED}Error: Failed to enable versioning${NC}"
fi

# Set lifecycle policy to clean up old versions
echo -e "${YELLOW}Setting lifecycle policy...${NC}"
cat > lifecycle-policy.json << EOF
{
    "Rules": [
        {
            "ID": "DeleteOldVersions",
            "Status": "Enabled",
            "Filter": {
                "Prefix": ""
            },
            "NoncurrentVersionExpiration": {
                "NoncurrentDays": 30
            },
            "AbortIncompleteMultipartUpload": {
                "DaysAfterInitiation": 1
            }
        }
    ]
}
EOF

aws s3api put-bucket-lifecycle-configuration \
    --bucket "$BUCKET_NAME" \
    --lifecycle-configuration file://lifecycle-policy.json

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Lifecycle policy set successfully${NC}"
    rm lifecycle-policy.json
else
    echo -e "${RED}Error: Failed to set lifecycle policy${NC}"
    rm lifecycle-policy.json
fi

echo -e "${GREEN}S3 bucket setup completed!${NC}"
echo "Bucket: $BUCKET_NAME"
echo "Region: $REGION"
echo "Versioning: Enabled"
echo "Lifecycle: Old versions deleted after 30 days"
