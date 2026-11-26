# Relatório de auditoria de segurança — Android

**Escopo:** revisão estática da implementação desta branch. Não é um pentest dinâmico nem substitui testes em dispositivo real.

## 1. Reconhecimento

- **Stack:** Kotlin, Android Gradle Plugin 8.13.2, Kotlin Gradle Plugin 2.4.21, AndroidX WebViewAssetLoader, JavaScript/HTML/CSS e ZIP local. JDK 17, Gradle 8.13.
- **Superfície:** Activity launcher; seletor de arquivos do Android; ponte JS AndroidBridge; Intents de compartilhamento; FileProvider não exportado; workflows de CI e release.
- **Autenticação/autorização:** o app não tem contas nem recursos remotos. O sistema operacional concede acesso somente aos URIs escolhidos no SAF picker. A Activity é exportada apenas para o launcher.
- **Dados sensíveis:** mídia selecionada é processada no WebView. Arquivos compartilhados são escritos em cacheDir/shared e expostos via FileProvider com URI grant de leitura. O cache é limpo por idade ao iniciar.
- **Custo/CPU:** não há endpoints, chamadas pagas, IA, SMS ou e-mail. A conversão consome CPU/memória locais. Limites: 150 MB por entrada, 10 segundos por clipe, 30 clipes, 75 MB agregado e 80 MiB na ponte nativa.
- **Segredos:** não há tokens/chaves no APK. A assinatura release usa KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS e KEY_PASSWORD como GitHub Actions secrets. O keystore é materializado no diretório temporário do runner e removido ao final.

## 2. Cinco verificações prioritárias

| Verificação | Resultado | Evidência |
|---|---|---|
| Rate limit / consumo sem limite | Não aplicável a serviço remoto | Não há login, cadastro, IA, SMS, e-mail, API ou backend. Limites locais ficam em app/src/main/assets/www/src/main.js e SharePayloadValidator.kt. |
| IDOR / BOLA | Não encontrado | Não existem rotas, objetos remotos ou IDs de recursos pertencentes a usuários. |
| Mass assignment | Não encontrado | Não há request body, ORM, persistência remota ou atualização de entidades. A ponte recebe apenas Base64, MIME e filename tipados como String? e valida os valores. |
| Webhook sem verificação | Não aplicável | Não há webhooks ou pagamentos. |
| Banco aberto | Não aplicável | Não há Supabase, Firebase, SQL ou banco de dados local. |

## 3. OWASP Top 10:2025

| Categoria | Resultado e evidência |
|---|---|
| A01 Broken Access Control | Nenhum recurso remoto/IDOR. Requisições fora da origem appassets são bloqueadas em app/src/main/java/com/erikraft/soundstickers/MainActivity.kt:100. FileProvider usa exported=false em app/src/main/AndroidManifest.xml:27 e só compartilha cache/shared em app/src/main/res/xml/file_paths.xml. |
| A02 Security Misconfiguration | Proteções confirmadas: allowFileAccess=false em MainActivity.kt:78; allowBackup=false e usesCleartextTraffic=false no Manifest; CSP meta em app/src/main/assets/www/index.html. O app não declara INTERNET. HSTS não se aplica sem servidor HTTP próprio. |
| A03 Software Supply Chain Failures | **PROVÁVEL / precisa de validação em CI:** versões diretas são fixadas, mas não há Gradle dependency lock nem verification metadata com hashes para o grafo transitivo. Os workflows usam tags principais das Actions (ex.: actions/checkout@v4) em vez de SHA imutável. Conferir ./gradlew app:dependencies e habilitar dependency verification após revisar os artefatos resolvidos; avaliar pinning dos SHAs das Actions. Não foi afirmada a existência de CVE sem executar a análise de dependências. |
| A04 Cryptographic Failures | Não foi encontrado segredo nem dado de pagamento. Mídias temporárias ficam em cache privado e não recebem criptografia adicional; risco residual caso o sandbox do Android esteja comprometido/rooted. |
| A05 Injection | Nenhum SQL/shell/template remoto/eval. O bridge restringe MIME, nome e Base64 em SharePayloadValidator.kt:20. Conteúdo de nome é renderizado com textContent; não forma HTML/código. |
| A06 Insecure Design | Limite do protocolo WhatsApp é explicitado: ACTION_SEND compartilha vídeo comum; não se promete importação pública de pacote sonoro. Há limites de tempo/tamanho/quantidade na interface e no bridge. |
| A07 Authentication Failures | Não há sessão ou autenticação; não aplicável a editor offline sem contas. |
| A08 Software or Data Integrity Failures | Release exige as quatro credenciais de assinatura no Gradle e no workflow; APK/AAB são verificados com apksigner/jarsigner. O wrapper recebe validação dedicada do Gradle. |
| A09 Security Logging and Alerting Failures | Não existe serviço remoto para logging/alerta. O código não registra Base64, conteúdo dos arquivos ou segredos. |
| A10 Mishandling of Exceptional Conditions | Picker cancelado, MIME/nome/Base64 inválidos, arquivos grandes, falha de escrita e ausência do app de destino têm tratamento de erro. |

## 4. Vulnerabilidades confirmadas e correções

**Nenhuma vulnerabilidade explorável foi confirmada nesta revisão estática do código novo.** Foram implementadas proteções preventivas: origem WebView restrita, CSP, sem permissão INTERNET, sem permissões amplas de storage, FileProvider não exportado, validação estrita na ponte, limites de mídia e assinatura de release.

## 5. Não verificável / ações externas necessárias

- Não foi executado em aparelho físico ou em várias versões do WebView/Android; validar codecs e fluxos de compartilhamento em devices reais.
- A análise CVE do grafo transitivo não foi executada; depende da resolução das dependências no CI.
- Regras de branch protection, permissões do GitHub Environment production e existência/escopo dos quatro secrets não podem ser confirmados pelo código. Conferir em Settings → Environments → production e Settings → Secrets and variables → Actions; não colocar valores em issues ou logs.
- A feature de sound sticker pode depender de rollout da conta/versão do WhatsApp.
