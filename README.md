# ErikrafT Sound Stickers for Android

Aplicativo Android nativo em Kotlin, com editor local de mídia integrado em uma WebView segura. A interface usa o editor de clipes sonoros e o player de áudio do projeto web, empacotados no APK para funcionar offline.

## Recursos

- Importação de vídeos, GIFs, imagens e áudios pelo seletor de arquivos do Android.
- Pré-visualização e player com progresso, volume, mute e waveform.
- FPS automático estimado pela cadência de quadros apresentada pelo vídeo, com escolha manual de 30 a 5 FPS.
- Ícone adaptativo Android (máscara quadrada/circular conforme o launcher) e camada monocromática para Themed Icons no Android 13 ou superior.
- Recorte de clipes de até 10 segundos.
- Substituição ou mixagem de áudio quando o WebView oferece suporte ao formato.
- Pacotes com até 30 clipes e limite agregado de 75 MB.
- Exportação ZIP com manifesto JSON e instruções.
- Botão WhatsApp em cada clipe: envia o vídeo ao WhatsApp instalado ou WhatsApp Business.
- Compartilhamento do ZIP pelo seletor de apps do Android.
- Processamento local; não exige conta, servidor, permissão ampla de armazenamento ou permissão de internet.

## Limite real da integração WhatsApp

O botão WhatsApp abre um diálogo do próprio app para escolher WhatsApp ou WhatsApp Business quando ambos estiverem instalados e compartilha o clipe MP4/WebM como mídia por meio de uma Intent ACTION_SEND. Esse diálogo não é o diálogo nativo de instalação de pacotes do WhatsApp e não instala uma figurinha sonora automaticamente.

O repositório oficial WhatsApp/stickers documenta integração de pacotes convencionais através de ContentProvider e metadados. Em 27 de novembro de 2026, o mantenedor confirmou que a API de figurinhas sonoras para terceiros ainda não está disponível e está no roadmap: https://github.com/WhatsApp/stickers/issues/1163. Por isso, o app não simula uma chamada ENABLE_STICKER_PACK como se ela importasse áudio. Depois de receber o vídeo, o usuário finaliza a criação no editor nativo do WhatsApp quando o recurso estiver disponível para sua conta.

O ZIP é um arquivo de organização/backup, não um pacote nativo importável pelo WhatsApp.

## Versão Web para iPhone e iPad

Para usar o editor em iPhone ou iPad pelo navegador, acesse [ErikrafT Sound Stickers — Web](https://sticker-ios.erikraft.com/). O código-fonte está em [erikraft/WA-Audio-Stickers-iOS](https://github.com/erikraft/WA-Audio-Stickers-iOS). É uma edição web independente deste aplicativo Android nativo; não é um APK nem um aplicativo publicado na App Store.

## Extensão de navegador de terceiros

Para o fluxo de integração com WhatsApp Web, consulte o projeto independente [vish-4-1/WA-audio-stickers-en](https://github.com/vish-4-1/WA-audio-stickers-en). **Esse repositório não pertence a ErikrafT e a extensão não foi criada, mantida ou oficialmente afiliada a ErikrafT.** Este app Android é uma implementação independente e não copia o código da extensão.

## Requisitos e desenvolvimento

- Android Studio com JDK 17.
- Android SDK Platform 36 e Build Tools 36.0.0.
- Android 7.0 (API 24) ou superior.

~~~bash
chmod +x gradlew
./gradlew test
./gradlew assembleDebug
./gradlew lintDebug
~~~

O APK de debug fica em app/build/outputs/apk/debug/app-debug.apk.

## Releases assinadas

O workflow .github/workflows/release.yml compila APK e AAB assinados ao publicar uma tag v* ou executar workflow_dispatch.

Configure estes secrets no GitHub Actions, preferencialmente no environment production:

- KEYSTORE_FILE: keystore Android codificado em Base64.
- KEYSTORE_PASSWORD: senha do keystore.
- KEY_ALIAS: alias da chave de assinatura.
- KEY_PASSWORD: senha da chave.

O workflow valida os secrets, mantém o keystore no diretório temporário do runner, verifica a assinatura do APK/AAB e remove o arquivo temporário ao final. Sem os secrets, o release assinado falha intencionalmente; a CI de PR continua usando build debug sem exigir credenciais.

## Segurança e privacidade

Veja SECURITY.md e docs/SECURITY_AUDIT.md. O app não declara permissão de internet. Os arquivos são processados localmente; só arquivos escolhidos pelo usuário são compartilhados. O FileProvider expõe apenas o diretório temporário cache/shared, com permissão de leitura concedida por URI.
