#!/bin/bash
sudo chmod -R 777 /tmp/cloudwatch-config.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/tmp/cloudwatch-config.json -s
sudo systemctl stop web
sudo chmod -R 777 /tmp/webApp-0.0.1-SNAPSHOT.jar
