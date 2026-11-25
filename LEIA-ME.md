# Moz Discount – projecto Android (APK)

Esta pasta contém uma app Android que abre o `preview.html` (em `app/src/main/assets/index.html`) dentro de um WebView.
Favoritos e filtros funcionam como na versão web; as contas ficam guardadas em SQLite (ver abaixo). O botão Voltar fecha primeiro as janelas abertas na app.

## Opção A – sem instalar nada (GitHub Actions)
1. Cria um repositório no GitHub (pode ser privado) e envia **o conteúdo desta pasta** (incluindo `.github`).
2. No GitHub: **Actions → Build APK → Run workflow** (também corre sozinho a cada push).
3. Quando acabar (cerca de 3–5 min), abre a execução e descarrega **MozDiscount-APK** em *Artifacts*. Dentro está `app-debug.apk`.
4. Passa o APK para o telemóvel e instala (ver abaixo).

## Opção B – Android Studio
1. Instala o Android Studio, **File → Open** e escolhe esta pasta. Deixa-o sincronizar o Gradle (descarrega o Gradle 8.7 e o SDK 34).
2. Liga o telemóvel (depuração USB) ou cria um emulador e carrega em **Run ▶**.
3. Para só gerar o ficheiro: **Build → Build APK(s)**. O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Se o GitHub criar um workflow "Android CI" por conta própria
Esse modelo corre `./gradlew build` (compila também release e lint) e **não guarda o APK**. Apaga-o (ou substitui o conteúdo
por `.github/workflows/build-apk.yml` desta pasta). O `gradlew` incluído serve de rede de segurança: descarrega o Gradle 8.7 sozinho.
Para gerar só o APK usa: `./gradlew assembleDebug`.

## Contas guardadas no telemóvel (SQLite)
- As contas e a sessão ficam numa base de dados **SQLite local** (`mozdiscount.db`, em `LocalDb.java`), só neste dispositivo.
- A palavra-passe **nunca** é guardada: só um hash PBKDF2-HMAC-SHA256 com sal aleatório por conta (120 000 iterações).
- Ao reabrir a app, a sessão é restaurada automaticamente. **Terminar sessão** limpa-a. No perfil há **Eliminar a minha conta**.
- A conta de developer vem embebida no `index.html` e não é guardada na base de dados.
- Os favoritos, cartões escolhidos, filtros e idioma continuam no armazenamento da página (localStorage).
- Requer Android 8.0 (API 26) ou superior. Desinstalar a app apaga a base de dados.
- No browser normal (sem a app) a página continua a guardar as contas só em memória.

## Instalar no telemóvel
Copia o `app-debug.apk` (WhatsApp, Drive, cabo...), abre-o e autoriza “Instalar apps desconhecidas” quando o Android pedir.

## Actualizar a app
Substitui `app/src/main/assets/index.html` pelo novo `preview.html` (com esse nome de destino) e volta a compilar.

## Notas
- É um APK de **debug** (assinado com a chave de debug do Android): serve para testes, não para a Google Play.
- A conta de developer vem embebida no `index.html`: não partilhes o APK publicamente.
- Sem internet a app funciona, só não carrega a fonte Google (usa uma fonte do sistema).
