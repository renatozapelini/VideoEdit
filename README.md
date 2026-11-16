# VideoEdit (Vídeo Edita Hub)

App Android com duas frentes para edição de vídeo:
- **Local (no aparelho):** seleção de vídeo, prévia de quadro e aplicação de borrão leve na região do rosto preservando a parte inferior (para manter a boca visível) e cobertura/borrão manual em área (apoio para tatuagem). Processamento local completo de vídeo/reastreio automático exige modelos/bibliotecas avançadas.
- **IA (ferramentas/serviços):** atalhos e fluxo para usar IAs que rastreiam e removem tatuagens/objetos e aplicam borrão facial preservando a boca.

## Requisitos atendidos no fluxo
- Rastrear e remover tatuagem: via ferramentas de IA com marcação/rastreio (seção IA); local oferece marcação/cobertura manual de apoio.
- Borrão leve no rosto mantendo boca visível: prévia local preserva faixa inferior da região facial; ferramentas de IA indicadas para detecção automática.

## Estrutura
- `AndroidManifest.xml`
- `app.properties`
- `java/com/buddyapps/videoedithub/*.java`
- `res/values/strings.xml`

## Build
Projeto em Java puro (sem Gradle/AndroidX). Pode ser compilado com os scripts da skill `android-app` (quando houver `aapt2` compatível) ou via GitHub Actions (workflow incluído, requer permissão `workflow` no token para push).
