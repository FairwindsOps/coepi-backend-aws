# TCN Server on AWS

This repo contains server and infrastructure code for deploying and running CoEpi Cloud API on AWS.

There are two API frontends provided: an AWS Lambda (api-aws-lambda) and a standalone server (api-server). For more information on running the standalone server, see [api-server/README.md](api-server/README.md).

* **Compute**: AWS Lambda OR any container-based solution
* **Data Store**: DynamoDB
* **Routing and Load Balancing**: API Gateway
* **Permissions Management**: IAM

## Getting Started

Requirements:

* JDK11 or newer

### Infrastructure Setup

All infrastructure concerns are managed by [Terraform](https://terraform.io/) (v0.12+). You can either download Terraform from their site, or install it with [Choco](https://chocolatey.org/) (Windows), [brew](https://brew.sh) (macOS), or your Linux distribution's package manager.

Terraform tracks the [state of your infrastructure](https://www.terraform.io/docs/state/index.html) in a `tfstate` file, which can be stored either locally or remotely. While we recommend remote state, both options are documented here for convenience.

#### Option 1: Remote Terraform State (Recommended)

First, create an S3 bucket to store your Terraform [remote state](https://www.terraform.io/docs/backends/index.html). This will need to be uniquely named, and should be private and encrypted. A script has been provided to do this for you:

```shell script
TFSTATE_BUCKET=your_bucket_name_here ./scripts/terraform_setup.sh
```

(If you're not sure what to name your bucket, try something like `<your github username>-tcn-infra`.)

With your bucket created, go to `./terraform` in the project root directory. Copy `backend.local.example` to `backend.local`. Edit the contents to update the `bucket` property to match the bucket name you created above. E.g.,

```text
bucket = "s3-bucket-with-your-terraform-state"
```

_(The `backend.local` file is specific to your development environment and is ignored by source control using `.gitignore`.)_

Finally, initialize your Terraform project:

```shell script
terraform init -backend-config=backend.local
```

#### Option 2: Local Terraform State

In a terminal, navigate to `./terraform` and run the following to initialize your Terraform state:

```shell script
terraform init
```

## Build

```sh
./gradlew build
```

Then build the shadow jar for the Lambda:

```sh
./gradlew api-aws-lambda:shadowJar
```

## Deploy

These steps assume you have already initialized Terraform as described in "Infrastructure Setup" above.

1. Ensure you have the AWS CLI configured, working, and pointing to the default AWS
   account you wish to deploy to.
2. Ensure you've run the "Build" step above
3. `cd` to the `terraform` folder in this repo
4. Run `terraform plan` to see what changes will be applied to
   your AWS account
5. Run `terraform apply -auto-approve` to make the changes and deploy the
   server.
6. When the Terraform scripts are updated or you wish to redeploy, repeat steps
   7 and 8.

The API Gateway root URL will be echoed to the shell, and you can CURL the
deployed API:

#### Base64 Encoded Report Data

```
h0NRnh3cJgUtHeI3oycAsJhWPsechtfiDyxkKQun9sV9lO0LhZfRRm4sMYGoSSOEBjj9qhGbvSookL/pBUkTwhQAKAAACkhpZ2ggRmV2ZXIOOyQmkj5pi/bM0Uo/h/4RxwH2wrN094GIOAQgMBCs8rCByJlxUqp+ZAJqgev2hC56nSHxbH0stERKps5GFI8E
```

#### v4

```sh
curl -X POST https://e6f2c4llfk.execute-api.us-west-1.amazonaws.com/v4/tcnreport/0.4.0 -d "<Base64 encoded report data>"
curl -X GET https://e6f2c4llfk.execute-api.us-west-1.amazonaws.com/v4/tcnreport/0.4.0
```

## Running

For running the server on your developer AWS account, follow the steps
below under ***AWS Insfrastructure Setup*** section.

For testing the lambda function and API locally, you can use SAM CLI. Below docs should be helpful.

https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html

https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-using-invoke.html 

## Documentation for API Endpoints

Swagger Definition and API documentation is located
under [**api_definition**](api_definition) folder:

The API can be tested by pasting the definition on [Swagger Editor](http://editor.swagger.io/)

### v4
Method | HTTP request | Description
------------- | ------------- | -------------
[**tcnreportPost**](docs/DefaultApi.md#cenreportpost) | **POST** /tcnreport/0.4.0 | Submit symptom or infection report following TCN 0.4.0 protocol
[**tcnreportGet**](docs/DefaultApi.md#cenreporttimestamplowertimestampupperget) | **GET** /tcnreport/0.4.0?intervalNumber={intervalNumber}?intervalLength={interval_length_seconds}| Returns a list of reports generated on the specified interval
