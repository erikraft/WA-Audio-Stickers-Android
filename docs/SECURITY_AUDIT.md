# Relatório de auditoria de segurança — Android

**Escopo:** revisão estática da implementação nativa desta branch. Não é um pentest dinâmico nem substitui testes em dispositivos reais.

## 1. Reconhecimento

- **Stack:** Kotlin, Android Gradle Plugin 8.13.2, Kotlin Gradle Plugin 2.3.21, AndroidX Media3 ExoPlayer/Transformer, Material Components 3, Storage Access Framework e FileProvider. JDK 17, Gradle 8.13.
- **Superfície:** Activity launcher; seletor de documentos do Android; decodificadores/encoders de mídia do dispositivo; exportação Media3; Intent de compartilhamento; FileProvider não exportado; workflows de CI e release.
- **Autenticação/autorização:** o app não possui contas ou recursos remotos. O Android concede acesso somente aos URIs escolhidos explicitamente pelo usuário. A Activity é exportada apenas para o launcher.
- **Dados sensíveis:** a mídia selecionada é processada localmente. O MP4 exportado é gravado em cacheDir/shared e exposto por FileProvider com concessão de leitura específica. O cache antigo é limpo na inicialização.
- **Consumo de recursos:** a transformação consome CPU, memória e bateria locais. Limites: 150 MB por arquivo de entrada, clipe de até 10 segundos e exportação temporária no cache privado.
- **Segredos:** não há tokens/chaves no APK. A assinatura release usa KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS e KEY_PASSWORD como GitHub Actions secrets. O keystore é materializado no diretório temporário do runner e removido ao final.
- **Rede:** o app não declara INTERNET e não depende de backend, conta, pagamento, webhook ou API de IA.

## 2. Verificações prioritárias

| Verificação | Resultado | Evidência |
|---|---|---|
| Rate limit / consumo remoto ilimitado | Não aplicável a serviço remoto | Não há login, cadastro, IA, SMS, e-mail, API ou backend. A transformação roda localmente. |
| IDOR / BOLA | Não encontrado | Não existem rotas, objetos remotos ou IDs de recursos pertencentes a usuários. |
| Mass assignment | Não encontrado | Não há request body, ORM, persistência remota ou atualização de entidades. Entradas são URIs selecionados via SAF e valores de recorte/FPS validados antes da exportação. |
| Webhook sem verificação | Não aplicável | Não há webhooks ou pagamentos. |
| Banco aberto | Não aplicável | Não há Supabase, Firebase, SQL ou banco de dados local. |

## 3. OWASP Top 10:2025

| Categoria | Resultado e evidência |
|---|---|
| A01 Broken Access Control | Nenhum recurso remoto/IDOR. A Activity launcher é a única Activity exportada. FileProvider usa exported=false e só compartilha cacheDir/shared. Os URIs de entrada vêm do seletor do Android. |
| A02 Security Misconfiguration | Proteções: allowBackup=false, usesCleartextTraffic=false, ausência da permissão INTERNET e FileProvider não exportado. O app não usa WebView. |
| A03 Software Supply Chain Failures | **PROVÁVEL / precisa de validação em CI:** versões diretas são fixadas, mas não há Gradle dependency lock nem verification metadata com hashes para o grafo transitivo. Actions usam tags principais em vez de SHA imutável. Conferir ./gradlew app:dependencies e habilitar dependency verification após revisar os artefatos; avaliar pinning dos SHAs das Actions. Não foi afirmada a existência de CVE sem executar análise de dependências. |
| A04 Cryptographic Failures | Não foi encontrado segredo nem dado de pagamento. Mídias temporárias ficam em cache privado e não recebem criptografia adicional; risco residual caso o sandbox Android esteja comprometido/rooted. |
| A05 Injection | Nenhum SQL/shell/template remoto/eval/JavaScript bridge. Nomes são tratados como texto e sanitizados antes de criar o nome do arquivo temporário. |
| A06 Insecure Design | O limite do protocolo WhatsApp é explicitado: ACTION_SEND compartilha vídeo comum; não se promete importação pública de pacote sonoro. Recorte, FPS e tipos de mídia são validados antes do Transformer. |
| A07 Authentication Failures | Não há sessão ou autenticação; não aplicável a editor offline sem contas. |
| A08 Software or Data Integrity Failures | Release exige as quatro credenciais de assinatura no Gradle e workflow; APK/AAB são verificados com apksigner/jarsigner. O wrapper recebe validação dedicada do Gradle. |
| A09 Security Logging and Alerting Failures | Não existe serviço remoto para logging/alerta. O código não registra conteúdo dos arquivos ou segredos. |
| A10 Mishandling of Exceptional Conditions | Picker cancelado, tipo de arquivo incompatível, arquivos grandes, metadados inválidos, falha de exportação e ausência do app de destino têm tratamento de erro. |

## 4. Vulnerabilidades confirmadas e correções

**Nenhuma vulnerabilidade explorável foi confirmada nesta revisão estática do código nativo.** O desenho reduz a superfície removendo WebView e a ponte JavaScript, restringindo entrada ao SAF, usando cache privado, FileProvider não exportado e validação de intervalos/taxas antes da exportação.

## 5. Não verificável / ações externas necessárias

- Não foi executado em aparelho físico ou em múltiplas versões de Android; validar decodificadores, encoders, exportações longas e compartilhamento real.
- A análise CVE do grafo transitivo não foi executada; depende da resolução das dependências no CI.
- Regras de branch protection, permissões do GitHub Environment production e existência/escopo dos quatro secrets não podem ser confirmados pelo código. Conferir em Settings → Environments → production e Settings → Secrets and variables → Actions; não colocar valores em issues ou logs.
- A disponibilidade da função de figurinha sonora depende de rollout do próprio WhatsApp; este app compartilha MP4 comum e não cria um áudio-sticker nativo.
