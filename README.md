## aws-cloudformation-resource-providers-network-firewall

This repository contains AWS-owned resource providers for the AWS::NETWORKFIREWALL::* namespace.

## Usage
The CloudFormation CLI (cfn) allows you to author your own resource providers that can be used by CloudFormation.

Refer to the documentation for the [CloudFormation CLI](https://github.com/aws-cloudformation/cloudformation-cli) for usage instructions.

## Development
First, you will need to install the [CloudFormation CLI](https://github.com/aws-cloudformation/cloudformation-cli), as it is a required dependency:
```
pip3 install cloudformation-cli
pip3 install cloudformation-cli-java-plugin
```

Linting is done via pre-commit, and is performed automatically on commit. The continuous integration also runs these checks.

```
pre-commit install
```

Manual options are available so you don't have to commit:
```
# run all hooks on all files, mirrors what the CI runs
pre-commit run --all-files
# run unit tests and coverage checks
mvn verify
```

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
