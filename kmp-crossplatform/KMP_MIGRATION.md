# Guia de Migração para Kotlin Multiplatform (KMP): Android, iOS & Web

Este diretório contém a estrutura de arquivos e as instruções necessárias para transformar o app **Zispr** em um aplicativo multiplataforma completo com **Compose Multiplatform** rodando nativamente no **Android, iOS e Navegadores Web (Wasm/JS)**.

---

## 🚀 Como Funciona o Ambiente do AI Studio
O editor do Google AI Studio em que você está possui um **ambiente de compilação em nuvem focado em Android**. Ele compila o código Kotlin + Jetpack Compose para APK e exibe o app no emulador de streaming do navegador.
* Como o ambiente não possui as ferramentas de compilação nativas da Apple (macOS/Xcode SDK), não é possível compilar o executável de iOS aqui dentro da nuvem.
* No entanto, organizamos os arquivos de configuração multiplataforma de forma perfeita aqui na raiz. Ao exportar o código (botão Exportar ou sincronizar com o GitHub), você poderá rodar o Zispr em um Mac ou na Web em segundos!

---

## 📁 Estrutura de Arquivos Multiplataforma Proposta
Para rodar em múltiplos targets, dividimos o projeto em:
1. `commonMain`: Código-fonte compartilhado do app (Telas em Jetpack Compose, ViewModels, Repositórios, Lógica do Player e Regras de Negócio).
2. `androidMain`: Código específico do Android (como permissões de sistema e inicialização do Application).
3. `iosMain`: Inicialização da interface no ecossistema iOS e ponte com Swift.
4. `wasmJsMain` (Web): Inicialização da interface renderizada em Canvas no navegador web.

---

## 🛠️ Configurações prontas neste pacote

Criamos as configurações e boilerplates universais para você:
* **`build.gradle.kts` Root e do `:composeApp`** preparados para Multiplatform.
* **`libs.versions.toml`** atualizado com as dependências do Compose Multiplatform da JetBrains e suporte a targets variados.
* **Pontos de Entrada Web** (`index.html` e `main.kt` para WebAssembly).
* **Pontos de Entrada iOS** (`main.ios.kt` convertendo a UI do Compose em um `UIViewController` para Swift).

---

## 🖥️ Como rodar o app multiplataforma localmente (Web/iOS/Android)

Depois de baixar o zip do projeto ou puxar para o GitHub, abra-o no **Android Studio** ou **IntelliJ IDEA**:

### 1. Rodar no Navegador (Web - Kotlin Wasm/JS)
Execute o seguinte comando no terminal na raiz do projeto:
```bash
./gradlew :composeApp:wasmJsRun --continuous
```
O Gradle abrirá um servidor local e compilará o app diretamente para WebAssembly (ótimo desempenho com renderização Skia no Canvas HTML).

### 2. Rodar no iOS (Simulador ou Dispositivo Físico)
No macOS com Xcode instalado:
1. Abra a pasta `iosApp` no Xcode.
2. Selecione o simulador (ex: iPhone 15) e clique no botão **Play** (Run).
3. Ou, via terminal do Android Studio:
```bash
./gradlew :composeApp:iosDeployToSimulator
```

### 3. Banco de Dados Compartilhado (Room KMP)
O **Room 2.7.0** usado no Zispr suporta KMP nativamente! No arquivo de banco de dados, em vez de instanciar o Database com um context de Android, use um getter multiplataforma:
```kotlin
// Em commonMain:
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
```
E implemente nos targets específicos a entrega do arquivo de dados (SQLite no Android e iOS, e SQLite persistido em IndexedDB na Web).
