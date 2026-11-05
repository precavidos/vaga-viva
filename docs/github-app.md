# Como criar o GitHub App do Vaga Viva

O Vaga Viva usa um GitHub App para criar o repositório do aluno na conta dele, a partir do repositório-modelo, e abrir uma issue por missão. Só uma pessoa do time precisa fazer isto, uma vez.

## 1. Criar o app

Em <https://github.com/organizations/precavidos/settings/apps/new> (ou em *Settings → Developer settings → GitHub Apps → New GitHub App* da organização), preencha:

| Campo | Valor |
| --- | --- |
| GitHub App name | `Vaga Viva` (ou outro nome livre no GitHub) |
| Homepage URL | `https://github.com/precavidos/vaga-viva` |
| Callback URL | `http://localhost:8080/login/oauth2/code/github` (acrescente a URL da demo quando houver deploy) |
| Expire user authorization tokens | marcado |
| Request user authorization (OAuth) during installation | marcado |
| Webhook → Active | desmarcado por enquanto (o revisor de PR, na semana 2, vai usar) |
| Where can this GitHub App be installed? | **Any account** (os alunos instalam na própria conta) |

## 2. Permissões

Em *Repository permissions*:

| Permissão | Acesso | Para quê |
| --- | --- | --- |
| Administration | Read and write | criar o repositório a partir do modelo |
| Contents | Read and write | ler o modelo e gravar `.vaga-viva/plan.json` |
| Issues | Read and write | abrir uma issue por missão |
| Metadata | Read-only | obrigatória em todo app |

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

A chave privada do app (*private key*) ainda não é necessária; ela entra junto com os webhooks do revisor.

## 4. Repositório-modelo

O repositório-modelo (por padrão `precavidos/vaga-viva-template-java-spring`, configurável em `vagaviva.github.template-repository`) precisa ser **público** e ter *Template repository* marcado em *Settings → General*. Ele segue o contrato do ADR 0005.

## 5. Como o aluno usa

1. O aluno clica em "Entrar com GitHub" na demo.
2. O GitHub pede para instalar o app na conta dele (escolher **All repositories**, para o app enxergar o repositório que vai ser criado) e autorizar.
3. O Vaga Viva cria o repositório na conta do aluno, grava o plano e abre as issues das missões.

## O que ainda falta confirmar com o app real

- Se o token do aluno consegue criar o repositório a partir de um modelo de outra conta (`precavidos`) quando o app está instalado só na conta do aluno.
- Se a permissão "Repository creation" existe no formulário e é necessária.

Se a criação falhar, o plano B do cronograma é criar o repositório na organização do projeto e convidar o aluno como colaborador.
