# ErikrafT Sound Stickers para Android

Editor Android nativo em Kotlin, com interface Material 3 verde/escura e processamento local usando AndroidX Media3 Transformer. O aplicativo não usa WebView, não carrega um site e não cria arquivos ZIP.

## Recursos nativos

- Seleção de vídeos e imagens pelo seletor de documentos do Android (Storage Access Framework).
- Pré-visualização de vídeo com Media3 ExoPlayer e pré-visualização de imagem.
- Recorte de vídeo com início/fim configuráveis e duração máxima de 10 segundos.
- Seleção de áudio externo para substituir o som original ou misturar as duas faixas.
- Para imagens estáticas, criação de um clipe MP4 com faixa de áudio selecionada.
- FPS máximo automático com base na taxa de quadros informada pelo vídeo, limitado a 30 FPS; escolha manual entre 30, 25, 20, 15, 12, 10, 8, 6 e 5 FPS.
- Imagens estáticas usam 5 FPS no modo automático.
- Exportação local para MP4 com a codificação disponível no dispositivo.
- Envio individual para WhatsApp ou WhatsApp Business. Se apenas um estiver instalado, o app abre diretamente esse destino; se ambos estiverem disponíveis, mostra a escolha.
- Ícone adaptativo Android e camada monocromática para Themed Icons no Android 13+.
- Sem conta, backend, permissão ampla de armazenamento ou permissão de internet.

## Limites reais da integração com WhatsApp

O app exporta e compartilha um vídeo MP4 por meio de uma Intent Android `ACTION_SEND`. O WhatsApp recebe esse vídeo e abre o fluxo normal de envio. **Isso não é a instalação de uma figurinha sonora e não abre um diálogo nativo de importação de áudio-sticker.**

A extensão de referência [vish-4-1/WA-audio-stickers-en](https://github.com/vish-4-1/WA-audio-stickers-en) usa módulos internos privados do WhatsApp Web (por exemplo, `WAWebSoundStickerUtils`, `WAWebUploadManager` e `MediaPrep.sendToChat`). Esses objetos não são uma API Android pública e não podem ser invocados a partir de um app nativo.

Em 27 de novembro de 2026, o mantenedor do repositório oficial [WhatsApp/stickers — issue #1163](https://github.com/WhatsApp/stickers/issues/1163) confirmou que a API pública de figurinhas sonoras para terceiros ainda não está disponível. Por isso, o app não finge que `ENABLE_STICKER_PACK` instala áudio nem tenta automatizar interfaces privadas do WhatsApp. A entrega suportada hoje é um MP4 comum.

## Compatibilidade de mídia

- Vídeos: os formatos dependem dos decodificadores disponíveis no Android/dispositivo.
- Imagens: o Media3 usa a decodificação de imagem do Android.
- GIF e outros formatos com múltiplos quadros são processados como um quadro estático pelo carregador padrão do Media3 nesta implementação nativa.
- Áudio externo: é recortado para a duração do clipe; quando é mais curto, pode ser repetido para preencher o trecho.
- A taxa escolhida no exportador é um teto de FPS. Ela pode reduzir a taxa de um vídeo de origem, mas não inventa quadros quando a origem tem FPS menor.

## Desenvolvimento

Requisitos:
- Android Studio e JDK 17.
- Android SDK Platform 36 e Build Tools 36.0.0.
- Android 7.0 (API 24) ou superior.

Comandos:

~~~bash
chmod +x gradlew
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
~~~

O APK de debug fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Releases assinadas

O workflow `.github/workflows/release.yml` compila APK e AAB assinados ao publicar uma tag `v*` ou executar `workflow_dispatch`.

Configure estes secrets no GitHub Actions, preferencialmente no environment production:
- `KEYSTORE_FILE`: keystore Android codificado em Base64.
- `KEYSTORE_PASSWORD`: senha do keystore.
- `KEY_ALIAS`: alias da chave de assinatura.
- `KEY_PASSWORD`: senha da chave.

O workflow valida os secrets, mantém o keystore no diretório temporário do runner, verifica a assinatura do APK/AAB e remove o arquivo temporário ao final. Sem os secrets, o release assinado falha intencionalmente; a CI de PR usa build debug sem exigir credenciais.

## Segurança e privacidade

O app só acessa os arquivos escolhidos pelo usuário no seletor do Android. A exportação acontece localmente e o MP4 temporário é disponibilizado ao app de destino por um FileProvider não exportado com permissão de leitura concedida por URI. Veja [SECURITY.md](SECURITY.md) e [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md).
