# Empresas seguidas

O aluno que recebe o e-mail semanal escolhe até 3 empresas. Todo domingo à noite o Vaga Viva lê a página de vagas delas, e o e-mail de segunda traz as vagas novas de tecnologia, cada uma com um botão "Montar meu plano para esta vaga".

O recurso é desligado por padrão. Ele só liga com `COMPANY_WATCH_SECRET` e `FIRECRAWL_API_KEY` cadastrados (veja [`deploy.md`](deploy.md)).

## Como funciona

1. **Seguir** (`/empresas`, com login do GitHub). O aluno cola o link da lista de vagas da empresa. O app lê a página na hora e grava todas as vagas abertas como já vistas, então elas não viram aviso. A resposta mostra "Encontramos N vagas abertas agora", com os links. O nome da empresa sai do link (subdomínio ou caminho), e o aluno pode mudar.
2. **Domingo, 22h (Brasília).** O workflow `company-watch.yml` chama `POST /api/company-watch/checks`. Cada página é lida uma vez, mesmo que vários alunos sigam a empresa. Os links de vaga da página são comparados com os já vistos. Das vagas novas, as 10 primeiras da página são lidas pelo mesmo leitor do link da vaga, e as de tecnologia (com ao menos uma tecnologia do vocabulário) viram aviso. As demais vagas novas são gravadas como vistas, sem aviso.
3. **Segunda.** O e-mail semanal ganha a seção "Vagas novas nas empresas que você segue", com no máximo 10 vagas. As que citam a stack do arquétipo do plano do aluno vêm primeiro, marcadas "combina com o seu plano". O botão abre o site com `#link=<vaga>`: a página pede o texto da vaga ao servidor e monta o plano.

## Plataformas aceitas

Só plataformas cujos links de vaga seguem um formato fixo. Assim, um link da página é ou não é uma vaga, sem precisar abri-lo.

| Plataforma | Página de vagas | Link de uma vaga |
|---|---|---|
| Gupy | `https://empresa.gupy.io/` | `/jobs/<número>` |
| Greenhouse | `https://job-boards.greenhouse.io/empresa` | `/empresa/jobs/<número>` |
| Lever | `https://jobs.lever.co/empresa` | `/empresa/<uuid>` |
| Ashby | `https://jobs.ashbyhq.com/empresa` | `/empresa/<uuid>` |
| Workable | `https://apply.workable.com/empresa/` | `/empresa/j/<código>` |
| SmartRecruiters | `https://careers.smartrecruiters.com/Empresa` | `jobs.smartrecruiters.com/Empresa/<número>-<título>` |
| Workday | `https://empresa.wd5.myworkdayjobs.com/Site` | `/Site/job/<local>/<título>_<id>` |

Qualquer outro endereço é recusado com uma mensagem que lista as plataformas. Algumas empresas usam uma dessas plataformas por trás de um site próprio. A página da Bosch no SmartRecruiters, por exemplo, leva ao `jobs.bosch.com`. Nesses casos, o aluno precisa do link da própria plataforma.

## Falhas

Uma página que não pôde ser lida soma uma falha seguida. Na 3ª falha seguida, o e-mail pede que o aluno confira o link, e a página `/empresas` mostra o mesmo aviso. A empresa nunca é removida sozinha. Uma leitura que dá certo zera a contagem.

Uma página que tinha vagas e de repente não lista nenhuma também conta como falha: o mais provável é que o link tenha mudado. Uma vaga nova cujo texto não pôde ser lido é gravada como vista, sem aviso, para não gastar toda semana uma das 10 leituras. Os links de uma página que está falhando não são apagados aos 90 dias, para que as vagas abertas não voltem todas como novas quando a leitura voltar.

O e-mail de segunda mostra as vagas encontradas nos 7 dias antes do envio. Se a leitura de domingo atrasar para depois das 8h de segunda (horário de Brasília), as vagas dela ficam para o e-mail seguinte.

## Custo

Cada página custa 1 crédito do Firecrawl por leitura (`/v2/scrape` com os formatos `markdown` e `links`, [preços](https://www.firecrawl.dev/pricing)). As vagas novas são lidas primeiro pelo download do próprio app, que não custa nada. Só a vaga que o app não consegue ler gasta 1 crédito. É o caso das vagas do Workable, então uma página do Workable pode custar até 11 créditos numa semana com 10 vagas novas. Para cerca de 100 alunos com 3 empresas cada, as leituras das páginas ficam perto de 600 créditos por mês, dentro dos 1.000 do plano gratuito. Empresas no Workable com muitas vagas novas podem passar disso: acompanhe o painel do Firecrawl. O botão do e-mail e a rota `/api/company-watch/posting-text` também leem a vaga, do mesmo jeito.

O `changeTracking` e o Monitor do Firecrawl foram descartados, porque comparar os links é mais simples e o GitHub Actions já agenda os outros envios.

## Dados

Os dados guardados estão na seção de privacidade do [README](../README.md#privacidade-e-lgpd). Em resumo:

- `company_follow`: login, página e nome. Sai ao deixar de seguir ou ao descadastrar do e-mail semanal (chave estrangeira com `on delete cascade`).
- `career_page`: o link público e as falhas seguidas, sem dado do aluno. Um gatilho apaga a página, com seus links, assim que ninguém mais a segue.
- `seen_posting`: os links (e títulos) das vagas já vistas, sem dado do aluno. Saem 90 dias depois da última vez que apareceram na página.

Quando um aluno passa a seguir uma página que outro já segue, as vagas abertas não são gravadas de novo. Assim, quem chegou antes ainda recebe as vagas que surgiram desde o domingo. Quem chegou depois pode ver na segunda uma vaga que já tinha aparecido na lista do "Encontramos N vagas".

## Testes

- `CareerPageAddressTests` e `ListedPostingsTests`: formato de página e de vaga por plataforma. As páginas salvas em `src/test/resources/watch/listings/` têm o formato da resposta do Firecrawl. As da Gupy, Greenhouse, Lever e Ashby trazem links e títulos reais. As do Workable, SmartRecruiters e Workday, que só aparecem com JavaScript, foram escritas à mão no formato real.
- `FirecrawlListingReaderTests`: o leitor da página contra um Firecrawl falso.
- `CompanyWatchApiTests`: seguir, a leitura de domingo e o que o e-mail recebe, contra o PostgreSQL.
- `CompanyWatchDisabledApiTests`: com o segredo vazio, nada muda.
