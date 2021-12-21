# JHipster generated Docker-Compose configuration

## Usage

Launch all your infrastructure by running: `docker-compose up -d`.

## Configured Docker services

### Service registry and configuration server:

- [Consul](http://localhost:8500)

### Applications and dependencies:

- gateway (gateway application)
- gateway's mysql database
- gateway's elasticsearch search engine
- todo (microservice application)
- todo's mysql database
- todo's elasticsearch search engine

### Additional Services:

- [Prometheus server](http://localhost:9090)
- [Prometheus Alertmanager](http://localhost:9093)
- [Grafana](http://localhost:3000)
- [Keycloak server](http://localhost:9080)
