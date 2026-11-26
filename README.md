# ErikrafT Sound Stickers for Android

Aplicativo Android nativo em Kotlin, com editor local de mídia integrado em uma WebView segura. A interface usa o editor de clipes sonoros e o player de áudio do projeto web, empacotados no APK para funcionar offline.

## Recursos

- Importação de vídeos, GIFs, imagens e áudios pelo seletor de arquivos do Android.
- Pré-visualização e player com progresso, volume, mute e waveform.
- Recorte de clipes de até 10 segundos.
- Substituição ou mixagem de áudio quando o WebView oferece suporte ao formato.
- Pacotes com até 30 clipes e limite agregado de 75 MB.
- Exportação ZIP com manifesto JSON e instruções.
- Botão WhatsApp em cada clipe: envia o vídeo ao WhatsApp instalado ou WhatsApp Business.
- Compartilhamento do ZIP pelo seletor de apps do Android.
- Processamento local; não exige conta, servidor, permissão ampla de armazenamento ou permissão de internet.

## Limite real da integração WhatsApp

O botão WhatsApp envia o clipe MP4/WebM como mídia por meio de uma Intent ACTION_SEND. Ele não abre diretamente o editor interno de criação de figurinha sonora.

O repositório oficial WhatsApp/stickers documenta integração de pacotes convencionais através de ContentProvider e metadados. Essa API pública não documenta importação de pacotes de figurinhas sonoras com áudio vinculado. Depois de receber o vídeo, o usuário finaliza a criação no editor nativo do WhatsApp quando o recurso estiver disponível para sua conta.

O ZIP é um arquivo de organização/backup, não um pacote nativo importável pelo WhatsApp.

## Extensão de navegador de terceiros

Para o fluxo de integração com WhatsApp Web, consulte https://github.com/vish-4-1/WA-audio-stickers-en. Essa extensão não foi criada, mantida, desenvolvida, pertence ou é oficialmente afiliada a ErikrafT. Este app é uma implementação independente e não copia o código da extensão.

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
