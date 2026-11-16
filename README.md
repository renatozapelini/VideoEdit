# VideoEdit (Vídeo Edita Hub)

App Android hub que reúne ferramentas grátis para:
- Desfocar/mascarar rosto em vídeo (ex.: CapCut, InShot, VN)
- Ajudar no fluxo para remover tatuagem/objetos em vídeo (edição com máscara/clone quadro a quadro; opções online)

## Estrutura
- `AndroidManifest.xml`
- `app.properties`
- `java/com/buddyapps/videoedithub/MainActivity.java`
- `res/values/strings.xml`

## Build
Projeto simples em Java puro (sem Gradle/AndroidX), pensado para build no ambiente Buddy com os scripts de `android-app` skill (`setup-toolchain.sh` e `build-apk.sh`), quando o toolchain compatível estiver disponível.

## Aviso
Este app é um hub: abre/reúne ferramentas; o processamento de vídeo é feito em cada ferramenta.
