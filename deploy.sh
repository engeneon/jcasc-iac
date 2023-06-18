#!/bin/bash
#simple demo deploy script

kubectl delete -f deployer.yaml

for i in `seq 1 6`
do
  kubectl get pods -n orion
  sleep 1
done
kubectl get services -n orion

kubectl apply -f deployer.yaml

for i in `seq 1 6`
do
  kubectl get pods -n orion
  sleep 1
done
kubectl get services -n orion
