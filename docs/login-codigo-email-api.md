# Login por código de e-mail

O mobile usa `POST /api/v1/auth/codigo/solicitar` com `email` e `perfil` (`motorista` ou `administrador`). A validação usa `POST /api/v1/auth/codigo/validar` com os mesmos campos e `codigo` de seis dígitos. A resposta da validação é o mesmo JSON do login por senha de cada perfil.

O código da API está no patch [api-login-codigo.patch](api-login-codigo.patch), baseado na `main` baixada em 1º de novembro de 2026. Aplique-o no repositório `Efficientia-API` antes de distribuir o mobile com o novo fluxo.

No Render, configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` e `MAIL_FROM` com o serviço SMTP. O código expira em dez minutos, permite cinco tentativas e pode ser reenviado após um minuto. A migração `V22__codigo_login_email.sql` cria o armazenamento necessário.
