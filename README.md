# LocalCam Viewer (Android · Kotlin + Media3 RTSP)

Aplicativo Android leve para visualizar **2 câmeras IP RTSP ao vivo** com vídeo e áudio, sem depender do app Yoosee, sem servidores externos e sem controles PTZ.

## Características principais
- **Media3 / ExoPlayer (`media3-exoplayer-rtsp`)** configurado com transporte padrão **UDP** (`setForceUseRtpTcp(false)`), evitando o erro `"Nonmatching transport in server reply"`.
- **Autenticação Digest e Basic** nativa via URI RTSP (`rtsp://usuario:senha@ip:porta/caminho`).
- **Áudio independente**: botão de ligar/desligar som em cada câmera + botão de reconectar + expandir tela cheia.
- **Armazenamento criptografado local**: nome, IP, porta, caminho, usuário e senha ficam salvos apenas no celular usando `EncryptedSharedPreferences` (Android Keystore). Nenhuma senha é gravada no código.
- **Compatível com rede local e Tailscale**: funciona direto no Wi-Fi de casa ou fora de casa via sub-rede anunciada pela TV Box no Tailscale.

## Compilação na nuvem (GitHub Actions)
1. Envie todos os arquivos deste projeto para um repositório no GitHub (incluindo a pasta `.github/workflows/`).
2. Abra a aba **Actions** no repositório.
3. Aguarde o workflow **Compilar APK Android** terminar (ou clique em **Run workflow**).
4. Baixe o arquivo **LocalCam-Viewer-APK** em *Artifacts*, descompacte o ZIP e instale o `app-debug.apk` no seu celular Android.
