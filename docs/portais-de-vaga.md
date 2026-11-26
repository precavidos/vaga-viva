# Portais de vaga: leitura pelo link

Teste de 26/11/2026: um link real de vaga de cada portal, enviado à API de produção (`POST /api/job-postings/extraction/from-url`), com o Firecrawl ligado.

A coluna "Quem leu" diz qual leitor deu o texto da vaga:

- **App:** o leitor do próprio app resolveu. Ele baixa a página e lê os dados estruturados da vaga (JSON-LD `JobPosting`) ou o texto principal.
- **Firecrawl:** o leitor do app não conseguiu, porque o site recusou o download ou a página é montada por JavaScript. O Firecrawl abriu a página num navegador e devolveu o texto.

Para saber quem leu, baixei cada página com o mesmo identificador do app (`VagaViva/0.1`) e conferi se ela tinha texto ou JSON-LD. Esse download saiu da máquina do time, e não do Render, então um site pode tratar os dois de forma diferente.

## Resultado

Todos os 15 links foram lidos.

| Portal | Exemplo testado | Quem leu | Observação |
|---|---|---|---|
| Gupy | Angular Front-End Developer, AI/ML Developer (Compass) | App | Toda vaga da Gupy traz JSON-LD. |
| LinkedIn Vagas | Software Engineer Django (BairesDev) | App | A página pública da vaga, sem login, traz JSON-LD. O LinkedIn pode limitar acessos repetidos. |
| Programathor | Back-end Sênior AI Engineer | App | |
| Trampos | Tech Lead | App | A página só tem o título visível, mas o JSON-LD tem a descrição inteira. |
| Vagas.com | Desenvolvedor de Software Jr | App | |
| InfoJobs | Desenvolvedora IA | App | |
| **Catho** | Desenvolvedor | **Firecrawl** | O site recusa o download do app (403). |
| **Indeed** | Vaga júnior | **Firecrawl** | O site recusa o download do app (401). |
| Greenhouse | Applied AI Engineer (GitLab) | App | Sem JSON-LD, mas com o texto na página. |
| Lever | Vaga da Palantir | App | |
| SmartRecruiters | Vaga da Bosch | App | Sem JSON-LD, mas com o texto na página. |
| **Workable** | Senior Software Engineer (Hugging Face) | **Firecrawl** | A página chega vazia, com 96 caracteres visíveis e sem JSON-LD. |
| Ashby | Fullstack Engineer (Linear) | App | A página é montada por JavaScript, mas traz JSON-LD. |
| Workday | Software Engineer (NVIDIA) | App | A página é montada por JavaScript, mas traz JSON-LD. |

## O que isso quer dizer

- **Sem o Firecrawl:** Catho, Indeed e Workable pedem que o aluno cole o texto da vaga.
- **Com o Firecrawl:** todos os portais testados funcionam pelo link.
- **Uma vaga lida não traz sempre competências:** a vaga é lida, mas só aparecem as competências que o vocabulário do arquétipo conhece. Vagas de outras áreas, como as de Agile Master e de ajudante de motorista na Gupy, ou a vaga administrativa da Palantir, voltam sem competências. É o comportamento certo para elas.
- **Vagas encerradas:** as páginas mudam e as vagas fecham. Um link que funcionou hoje pode dar "vaga não encontrada" amanhã. Isso não é falha do leitor.
- **Fora do teste:** Glassdoor, Sólides e BNE.
