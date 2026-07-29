# Guia de Preservação do ID do Pacote (Application ID) - Tempo Track

Este documento foi criado a pedido do usuário para identificar e garantir que o identificador único do aplicativo (**Application ID**) seja mantido inalterado em todas as atualizações futuras e builds via GitHub ou localmente.

## ID Obrigatório de Uso do Aplicativo
O aplicativo **Tempo Track** deve usar obrigatoriamente o seguinte ID:

```kotlin
applicationId = "com.aistudio.tempotrack.kzmplo"
```

## Onde ele é configurado?
No arquivo `app/build.gradle.kts`, dentro do bloco `android.defaultConfig`:

```kotlin
android {
    ...
    defaultConfig {
        applicationId = "com.aistudio.tempotrack.kzmplo" // <- NUNCA ALTERAR ESTA LINHA
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
        ...
    }
}
```

## Por que este ID é obrigatório?
Para que o Android permita a atualização direta de um aplicativo já instalado no telefone do usuário (seja compilado pelo PC ou baixado por um fluxo de CI/CD do GitHub), duas coisas são mandatórias:
1. **O ID do pacote (`applicationId`) precisa ser idêntico.** Se o ID mudar, o Android considerará como um aplicativo totalmente diferente e não atualizará o aplicativo atual.
2. **A assinatura do APK (`debug.keystore` ou chave de produção) deve ser a mesma.** Caso haja conflito de assinatura mesmo com o ID igual, o Android exibirá a mensagem: *"Como o pacote tem um conflito com um pacote já existente, o app não foi instalado"*.

Se o ID acima for modificado acidentalmente em um refatoramento ou por algum assistente, reverta a alteração de imediato para manter a compatibilidade de atualização com o repositório principal no GitHub.
