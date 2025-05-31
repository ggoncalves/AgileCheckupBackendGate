#!/bin/bash
set -e

FUNCTION_NAME="AgileCheckupApi"
REGION="us-east-1"

echo "Building Lambda package..."
mvn clean package

echo "Deploying to AWS Lambda..."
aws lambda update-function-code \
    --function-name $FUNCTION_NAME \
    --zip-file fileb://target/agilecheckup-api-1.0-SNAPSHOT.jar \
    --region $REGION

echo "Deployment completed successfully!"

# Optional: Update function configuration if needed
# aws lambda update-function-configuration \
#     --function-name $FUNCTION_NAME \
#     --timeout 30 \
#     --memory-size 512 \
#     --region $REGION
