# LeisureMeta

## LM Scan

### Dev Mode

#### Run Backend

```bash
sbt ~lmscanBackend/reStart
```

#### Run ScalaJS

```bash
sbt ~lmscanFrontend/fastLinkJS
```

#### Run Frontend

```bash
cd modules/lmscan-frontend
yarn start
```

### Build Mode

#### Assembly Backend

```bash
sbt lmscanBackend/assembly
```

#### Build ScalaJS

```bash
sbt lmscanFrontend/fullLinkJS
```

#### Build Frontend

```bash
cd modules/lmscan-frontend
yarn build
```
