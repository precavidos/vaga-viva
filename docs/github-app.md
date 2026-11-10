# Como criar o GitHub App do Vaga Viva

O Vaga Viva usa um GitHub App para criar o repositório do aluno na conta dele, a partir do repositório-modelo, e abrir uma issue por missão. Só uma pessoa do time precisa fazer isto, uma vez.

## 1. Criar o app

Em <https://github.com/organizations/precavidos/settings/apps/new> (ou em *Settings → Developer settings → GitHub Apps → New GitHub App* da organização), preencha:

| Campo | Valor |
| --- | --- |
| GitHub App name | `Vaga Viva` (ou outro nome livre no GitHub) |
| Homepage URL | `https://github.com/precavidos/vaga-viva` |
| Callback URL | `http://localhost:8080/login/oauth2/code/github` e `https://vaga-viva.onrender.com/login/oauth2/code/github` |
| Expire user authorization tokens | marcado |
| Request user authorization (OAuth) during installation | **desmarcado** (o login é feito antes, pelo Vaga Viva; com esta opção, o GitHub volta sem o `state` que o login exige) |
| Setup URL (Post installation) | `https://vaga-viva.onrender.com/?instalado=1` |
| Redirect on update | marcado |
| Webhook → Active | **marcado** |
| Webhook URL | `https://vaga-viva.onrender.com/api/github/webhook` |
| Webhook secret | um texto aleatório longo, o mesmo de `GITHUB_WEBHOOK_SECRET` no Render (gere com `openssl rand -hex 32`) |
| Where can this GitHub App be installed? | **Any account** (os alunos instalam na própria conta) |

## 2. Permissões

Em *Repository permissions*:

| Permissão | Acesso | Para quê |
| --- | --- | --- |
| Administration | Read and write | criar o repositório a partir do modelo |
| Contents | Read and write | ler o modelo e gravar `.vaga-viva/plan.json` |
| Issues | Read and write | abrir uma issue por missão |
| Metadata | Read-only | obrigatória em todo app |
| Pull requests | Read and write | o revisor lê o diff e comenta no PR do aluno |
| Actions | Read-only | o revisor baixa o relatório de testes do workflow `Vaga Viva` |

Em *Subscribe to events*, marque **Pull request**, **Workflow run** e **Issue comment**. Mudar permissões pede que cada conta que já instalou o app aceite as novas na página da instalação; o GitHub avisa o dono da conta.

A documentação do GitHub também lista uma permissão "Repository creation" (write) para criar repositório a partir de modelo. Se ela aparecer no formulário, marque *Read and write*. Se não aparecer, siga sem ela e anote; o teste com o app de verdade vai dizer se falta algo.

Nenhuma permissão de organização ou de conta é necessária.

## 3. Guardar as credenciais

Depois de criar o app:

1. Anote o **Client ID**.
2. Em *Client secrets*, gere um **client secret** e copie na hora (ele não aparece de novo).

Nunca coloque esses valores no repositório nem no chat. Na sua máquina, deixe-os em variáveis de ambiente:

```bash
export GITHUB_CLIENT_ID=...
export GITHUB_CLIENT_SECRET=...
```

Há um modelo em `.env.example`; o `.env` está no `.gitignore`.

Para o revisor, o app precisa também da **chave privada** e do **App ID**:

1. Em *General → Private keys*, clique em **Generate a private key**. O GitHub baixa um arquivo `.pem`; guarde-o fora do repositório (arquivos `*.pem` estão no `.gitignore`).
2. Anote o **App ID**, no topo da mesma página.
3. No Render, cadastre `GITHUB_APP_ID`, `GITHUB_APP_PRIVATE_KEY` (o conteúdo do `.pem`, colado como está; quebras de linha escritas como `\n` também funcionam) e `GITHUB_WEBHOOK_SECRET`.

Sem essas três variáveis, a API sobe normalmente e o webhook responde 503. Com elas, cada entrega precisa da assinatura `X-Hub-Signature-256` certa, ou é recusada com 401.

## 4. Repositório-modelo

O repositório-modelo (por padrão `precavidos/vaga-viva-template-java-spring`, configurável em `vagaviva.github.template-repository`) precisa ser **público** e ter *Template repository* marcado em *Settings → General*. Ele segue o contrato do ADR 0005.

## 5. Como o aluno usa

1. Na página do Vaga Viva, o aluno cola a vaga, vê o plano e clica em "Criar meu repositório no GitHub".
2. Se ainda não entrou, vai para o login do GitHub e volta para a página (`/?entrou=1`).
3. Se o app ainda não está instalado na conta dele, a página o leva para a instalação (escolher **All repositories**, para o app enxergar o repositório que vai ser criado). A Setup URL o traz de volta (`/?instalado=1`).
4. O Vaga Viva cria o repositório na conta do aluno, grava o plano e abre as issues das missões.

Enquanto o aluno entra e instala, o texto da vaga fica só no navegador dele (`sessionStorage`) e é apagado quando o repositório é criado. O token do GitHub fica na sessão do servidor, em memória, e não é gravado nem registrado no log (ADR 0003).

As variáveis do Render são `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` e, se o app não se chamar `vaga-viva` no GitHub, `GITHUB_APP_SLUG`.

## O que ainda falta confirmar com o app real

- Se o token do aluno consegue criar o repositório a partir de um modelo de outra conta (`precavidos`) quando o app está instalado só na conta do aluno.
- Se a permissão "Repository creation" existe no formulário e é necessária.

Se a criação falhar, o plano B do cronograma é criar o repositório na organização do projeto e convidar o aluno como colaborador.
