#!/usr/bin/env bash
printf "Configuring localstack components..."

readonly LOCALSTACK_SQS_URL=http://localstack:4566

sleep 5;

set -x

aws configure set aws_access_key_id C8FJ5CYT8OI9CSKZL0XER
aws configure set aws_secret_access_key cdt+GrfhDulO9H556bc000OlAcxkilbgErXcdFRT
echo "[default]" > ~/.aws/config
echo "region = us-east-1" >> ~/.aws/config
echo "output = json" >> ~/.aws/config

aws --endpoint $LOCALSTACK_SQS_URL sqs create-queue --queue-name custom-signal
aws --endpoint $LOCALSTACK_SQS_URL sqs create-queue --queue-name custom-signal-in.fifo --attributes FifoQueue=true,MessageGroupId=signal


# aws --endpoint http://localstack:4566 sqs create-queue --queue-name custom-signal
# aws --endpoint http://localstack:4566 sqs create-queue --queue-name custom-signal-in.fifo --attributes FifoQueue=true,MessageGroupId=signal


# printf "Sample data begin..."
# # create tmp directory to put sample data after chunking
# mkdir -p /tmp/localstack/data
# # aws s3 cp --debug "s3://nyc-tlc/trip data/yellow_tripdata_2018-04.csv" /tmp/localstack --no-sign-request --region us-east-1
# aws --endpoint-url=$LOCALSTACK_S3_URL s3api create-bucket --bucket nyc-tlc
# # Create lambda deploy bucket for our simple http endpoint example
# aws --endpoint-url=$LOCALSTACK_S3_URL s3api create-bucket --bucket simple-http-endpoint-local-deploy
# # Grant bucket public read
# aws --endpoint-url=$LOCALSTACK_S3_URL s3api put-bucket-acl --bucket nyc-tlc --acl public-read
# aws --endpoint-url=$LOCALSTACK_S3_URL s3api put-bucket-acl --bucket simple-http-endpoint-local-deploy --acl public-read
# # Create a folder inside the bucket
# aws --endpoint-url=$LOCALSTACK_S3_URL s3api put-object --bucket nyc-tlc --key "trip data/"
# aws --endpoint-url=$LOCALSTACK_S3_URL s3 sync /tmp/localstack "s3://nyc-tlc/trip data" --cli-connect-timeout 0
# # Display bucket content
# aws --endpoint-url=$LOCALSTACK_S3_URL s3 ls "s3://nyc-tlc/trip data"

set +x

# This is the localstack dashboard, its pretty useless so get ready to learn how to use AWS Cli well!
printf "Localstack dashboard : http://localhost:8080/#!/infra"