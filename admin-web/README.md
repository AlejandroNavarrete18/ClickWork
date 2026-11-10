# FixNear Admin - starter

Este es un punto de partida visual del panel administrativo, no un panel conectado a producción.

```bash
cd admin-web
npm install
cp .env.example .env
npm run dev
```

No agregues una clave `service_role` al `.env` del navegador. Antes de conectar datos, implementa autenticación de administradores, RLS y Edge Functions para operaciones privilegiadas.
