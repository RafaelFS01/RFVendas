package BackEnd.util;

import BackEnd.model.entity.Cliente;
import BackEnd.model.entity.ItemPedido;
import BackEnd.model.entity.Pedido;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gera um arquivo PDF representando um Pedido (ou formulário de aluguel),
 * baseado em um layout específico.
 * Alguns campos são preenchidos automaticamente a partir do objeto Pedido,
 * enquanto outros (RG, Tel(c), Retirada, Entrada, Restante, Atendente)
 * são deixados com espaço para preenchimento manual.
 */
public class PedidoPdfGenerator {

    // --- Constantes de Formatação e Layout ---
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRAZIL_LOCALE = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);
    private static final float MARGIN = 50; // Margem em pontos (pt). 1 polegada = 72 pt. ~17.6mm
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final String MANUAL_DATE_PLACEHOLDER = "__/__/____";
    private static final String MANUAL_TEXT_PLACEHOLDER = ""; // Deixar em branco, a linha indica o campo

    // Fontes e Tamanhos
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float FONT_SIZE_NORMAL = 10;
    private static final float FONT_SIZE_SMALL = 8;
    private static final float FONT_SIZE_LARGE = 12;
    private static final float FONT_SIZE_HEADER = 9; // Para endereço da loja

    // Espaçamentos
    private static final float LINE_SPACING = 15; // Espaçamento padrão entre linhas de campos
    private static final float ADDR_LINE_SPACING = 11; // Espaçamento menor para endereço da loja
    private static final float ITEM_LINE_SPACING = 14; // Espaçamento para itens da lista
    private static final float TERMS_LINE_SPACING = 10; // Espaçamento para o texto dos termos

    // Comprimento das linhas para campos (ajustar conforme necessário para o visual)
    private static final float LINE_LENGTH_SHORT = CONTENT_WIDTH * 0.30f; // Para colunas (RG, Tel, Entrada, etc.)
    private static final float LINE_LENGTH_MEDIUM = CONTENT_WIDTH * 0.40f;
    private static final float LINE_LENGTH_LONG = CONTENT_WIDTH * 0.80f; // Para campos largos (Locatario, Endereço)
    private static final float LINE_LENGTH_ITEM_DESC = CONTENT_WIDTH * 0.65f;
    private static final float LINE_LENGTH_ITEM_QTY = 40f;
    private static final float LINE_LENGTH_SIGNATURE = CONTENT_WIDTH * 0.6f;

    /**
     * Gera o arquivo PDF para um determinado Pedido.
     *
     * @param pedido   O objeto Pedido contendo os dados a serem preenchidos.
     * @param filePath O caminho completo onde o arquivo PDF será salvo.
     * @throws IOException              Se ocorrer um erro de I/O durante a criação ou salvamento do PDF.
     * @throws IllegalArgumentException Se o pedido ou cliente forem nulos.
     */
    public void generatePdf(Pedido pedido, String filePath) throws IOException {

        if (pedido == null) {
            throw new IllegalArgumentException("Objeto Pedido não pode ser nulo para gerar PDF.");
        }
        if (pedido.getCliente() == null) {
            throw new IllegalArgumentException("Cliente associado ao pedido não pode ser nulo.");
        }

        // Usa try-with-resources SÓ para o PDDocument (que é criado uma vez)
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = null; // Declare a variável ANTES do try
            float yPosition = PAGE_HEIGHT - MARGIN;   // Inicializa a posição Y

            try { // Inicia um bloco try normal para gerenciar o stream manualmente
                contentStream = new PDPageContentStream(document, page); // Cria o PRIMEIRO stream

                // --- Desenha o Cabeçalho (Logo e Endereço) ---
                yPosition = drawHeader(document, contentStream, yPosition);
                yPosition -= LINE_SPACING * 1.5f; // Espaço extra após cabeçalho

                // --- Desenha as Informações do Cliente e Pedido ---
                yPosition = drawClientAndOrderInfo(contentStream, pedido, yPosition);
                yPosition -= LINE_SPACING * 2f; // Espaço antes da lista de itens

                // --- Título da Lista de Itens ---
                drawCenteredText(contentStream, "ESPECIFICAÇÕES DOS ITENS", FONT_BOLD, FONT_SIZE_LARGE, yPosition, PAGE_WIDTH);
                yPosition -= LINE_SPACING * 1.5f;

                // --- Desenha a Lista de Itens (com paginação interna) ---
                // O método drawItemListAndPaginate agora trata a criação/fechamento de streams e páginas
                Object[] result = drawItemListAndPaginate(document, page, contentStream, pedido, yPosition);
                page = (PDPage) result[0]; // Atualiza a página atual
                contentStream = (PDPageContentStream) result[1]; // Atualiza o stream atual
                yPosition = (float) result[2]; // Atualiza a posição Y atual

                yPosition -= LINE_SPACING * 1.5f; // Espaço antes dos termos


                // --- Desenha os Termos e a Linha de Assinatura ---
                // Verifica se há espaço suficiente ANTES de desenhar os termos
                float estimatedTermsHeight = calculateApproxTextHeight(getTermsText(pedido.getCliente()), CONTENT_WIDTH, FONT_REGULAR, FONT_SIZE_SMALL, TERMS_LINE_SPACING);
                estimatedTermsHeight += LINE_SPACING * 3; // Adiciona espaço para assinatura e margem inferior

                if (yPosition < MARGIN + estimatedTermsHeight) {
                    System.out.println("Adicionando nova página ANTES dos Termos.");
                    contentStream.close(); // Fecha o stream ATUAL
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page); // Cria um NOVO stream para a nova página
                    yPosition = PAGE_HEIGHT - MARGIN; // Reinicia Y no topo da nova página
                }

                // Agora desenha os termos e assinatura na página/stream correto(a)
                drawTermsAndSignature(contentStream, pedido.getCliente(), yPosition);

            } finally {
                // Bloco finally para GARANTIR que o ÚLTIMO contentStream seja fechado
                if (contentStream != null) {
                    contentStream.close();
                }
            } // Fim do try/finally para contentStream

            // Salva o documento no caminho especificado
            document.save(filePath);
            System.out.println("PDF gerado com sucesso em: " + filePath);

        } // Fecha document automaticamente (try-with-resources)
    }

    // =========================================================================
    // Métodos Auxiliares para Desenhar Seções
    // =========================================================================

    /**
     * Desenha o cabeçalho do PDF, incluindo o logo e o endereço da loja.
     */
    private float drawHeader(PDDocument document, PDPageContentStream stream, float yStart) throws IOException {
        float currentY = yStart;
        float centerPage = PAGE_WIDTH / 2;

        // 1. Desenhar Logo (Carregar de resources/images)
        try (InputStream logoStream = getClass().getResourceAsStream("/images/LL_Noivas_PDF.png")) { // Ajuste o caminho
            if (logoStream != null) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, logoStream.readAllBytes(), "logo");
                float logoWidth = 130; // Largura desejada em pt
                float logoHeight = (logoWidth / pdImage.getWidth()) * pdImage.getHeight();
                float logoX = centerPage - logoWidth / 2;
                stream.drawImage(pdImage, logoX, currentY - logoHeight, logoWidth, logoHeight);
                currentY -= (logoHeight + 10); // Atualiza Y
            } else {
                handleMissingResource("Logo", "/images/LL_Noivas_PDF.png");
                currentY = drawPlaceholderLogo(stream, currentY, centerPage);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar/desenhar logo: " + e.getMessage());
            e.printStackTrace();
            currentY -= 70; // Pula espaço mesmo em erro
        }

        // 2. Desenhar Endereço da Loja (Centralizado)
        String[] addressLines = {
                "Praca João Barbosa de Carvalho loja 02 N 30",
                "Centro(Praça do Forúm)-Feira de Santana-BA",
                "Tel:(75) 99106-9775"
        };
        for (String line : addressLines) {
            drawCenteredText(stream, line, FONT_REGULAR, FONT_SIZE_HEADER, currentY, PAGE_WIDTH);
            currentY -= ADDR_LINE_SPACING;
        }

        return currentY; // Retorna a posição Y final do cabeçalho
    }

    /** Desenha um placeholder caso o logo não seja encontrado. */
    private float drawPlaceholderLogo(PDPageContentStream stream, float yStart, float centerPage) throws IOException {
        float currentY = yStart;
        float logoPlaceholderSize = 60;
        stream.setLineWidth(1.5f);
        // Desenha um círculo simples com 'L' dentro
        // stream.ellipse(centerPage - logoPlaceholderSize/2, currentY - logoPlaceholderSize, logoPlaceholderSize/2, logoPlaceholderSize/2);
        stream.stroke();
        drawCenteredText(stream, "L", FONT_BOLD, 40, currentY - logoPlaceholderSize * 0.75f , PAGE_WIDTH); // Ajuste Y do 'L'
        currentY -= (logoPlaceholderSize + 10);
        return currentY;
    }

    /** Loga um aviso sobre recurso não encontrado. */
    private void handleMissingResource(String resourceName, String path) {
        System.err.println("AVISO: Recurso '" + resourceName + "' não encontrado em '" + path + "'. Verifique o caminho e se o arquivo está nos resources compilados.");
    }


    /**
     * Desenha a seção com informações do cliente e detalhes do pedido.
     */
    private float drawClientAndOrderInfo(PDPageContentStream stream, Pedido pedido, float yStart) throws IOException {
        float currentY = yStart;
        float firstColX = MARGIN;
        float secondColX = MARGIN + CONTENT_WIDTH / 2 + 10;

        Cliente cliente = pedido.getCliente();

        // Linha 1: Locatário (Automático)
        currentY = drawLabelAndValueWithLine(stream, "Locatário:", cliente.getNome(), firstColX, currentY, LINE_LENGTH_LONG);
        // Linha 2: RG (Manual) / CPF (Automático)
        currentY = drawTwoColumnsWithLines(stream, "RG:", MANUAL_TEXT_PLACEHOLDER, LINE_LENGTH_SHORT, "CPF:", cliente.getCpfCnpj(), LINE_LENGTH_SHORT, firstColX, secondColX, currentY);
        // Linha 3: Telefone (Automático) / Tel(c) (Manual)
        currentY = drawTwoColumnsWithLines(stream, "Telefone:", cliente.getTelefoneCelular(), LINE_LENGTH_SHORT, "Tel(c):", MANUAL_TEXT_PLACEHOLDER, LINE_LENGTH_SHORT, firstColX, secondColX, currentY);
        // Linha 4: Cidade (Automático) / Bairro (Automático)
        currentY = drawTwoColumnsWithLines(stream, "Cidade:", cliente.getCidade(), LINE_LENGTH_SHORT, "Bairro:", cliente.getBairro(), LINE_LENGTH_SHORT, firstColX, secondColX, currentY);
        // Linha 5: Endereço (Automático)
        String enderecoCompleto = formatAddress(cliente);
        currentY = drawLabelAndValueWithLine(stream, "Endereço:", enderecoCompleto, firstColX, currentY, LINE_LENGTH_LONG);
        // Linha 6: Datas (Data Aluguel e Devolução automáticas, Retirada manual)
        currentY = drawDateFields(stream, pedido, firstColX, currentY);
        // Linha 7: Valor Aluguel (Automático) / Entrada (Manual)
        currentY = drawTwoColumnsWithLines(stream, "Valor do aluguel:", CURRENCY_FORMATTER.format(pedido.getValorTotal()), LINE_LENGTH_SHORT, "Entrada: R$", MANUAL_TEXT_PLACEHOLDER, LINE_LENGTH_SHORT, firstColX, secondColX, currentY);
        // Linha 8: Atendente (Manual) / Restante (Manual)
        currentY = drawTwoColumnsWithLines(stream, "Atendente:", MANUAL_TEXT_PLACEHOLDER, LINE_LENGTH_MEDIUM, "Restante: R$", MANUAL_TEXT_PLACEHOLDER, LINE_LENGTH_SHORT, firstColX, secondColX, currentY);

        return currentY;
    }

    /** Formata o endereço completo do cliente. */
    private String formatAddress(Cliente cliente) {
        StringBuilder sb = new StringBuilder();
        if (cliente.getLogradouro() != null && !cliente.getLogradouro().isBlank()) sb.append(cliente.getLogradouro());
        if (cliente.getNumero() != null && !cliente.getNumero().isBlank()) sb.append(", ").append(cliente.getNumero());
        if (cliente.getComplemento() != null && !cliente.getComplemento().isBlank()) sb.append(" - ").append(cliente.getComplemento());
        return sb.toString().trim();
    }

    /** Desenha os campos de data na linha correspondente. */
    private float drawDateFields(PDPageContentStream stream, Pedido pedido, float xStart, float yStart) throws IOException {
        float currentY = yStart;
        // Ajustar posições X para melhor distribuição
        float x1 = xStart;
        float x2 = xStart + CONTENT_WIDTH * 0.38f;
        float x3 = xStart + CONTENT_WIDTH * 0.70f;

        // Data Aluguel (Automático)
        String dataAluguelStr = pedido.getDataPedido() != null ? pedido.getDataPedido().format(DATE_FORMATTER) : MANUAL_DATE_PLACEHOLDER;
        drawText(stream, "Data do Aluguel: " + dataAluguelStr, FONT_REGULAR, FONT_SIZE_NORMAL, x1, currentY);

        // Retirada (Manual Placeholder)
        drawText(stream, "Retirada: " + MANUAL_DATE_PLACEHOLDER, FONT_REGULAR, FONT_SIZE_NORMAL, x2, currentY);

        // Devolução (Automático com tratamento de nulo)
        String dataDevolucaoStr = pedido.getDataRetorno() != null ? pedido.getDataRetorno().format(DATE_FORMATTER) : MANUAL_DATE_PLACEHOLDER;
        drawText(stream, "Devolução: " + dataDevolucaoStr, FONT_REGULAR, FONT_SIZE_NORMAL, x3, currentY);

        return currentY - LINE_SPACING; // Retorna a próxima posição Y
    }

    /**
     * Desenha a lista de itens do pedido, tratando a paginação.
     * Retorna o estado atualizado da página, stream e posição Y.
     */
    private Object[] drawItemListAndPaginate(PDDocument document, PDPage currentPage, PDPageContentStream currentStream, Pedido pedido, float yStart) throws IOException {
        float currentY = yStart;
        float itemNumberX = MARGIN;
        float descriptionX = MARGIN + 20;
        float qtyLabelX = PAGE_WIDTH - MARGIN - LINE_LENGTH_ITEM_QTY - 35;
        float qtyValueX = qtyLabelX + 30;

        int itemCounter = 1;
        final int minLines = 3; // Mínimo de linhas a serem exibidas

        if (pedido.getItens() != null) {
            for (ItemPedido itemPedido : pedido.getItens()) {
                // Verifica se precisa de nova página ANTES de desenhar
                if (currentY < MARGIN + ITEM_LINE_SPACING) {
                    currentStream.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    currentStream = new PDPageContentStream(document, currentPage);
                    currentY = PAGE_HEIGHT - MARGIN;
                    System.out.println("Adicionada nova página para continuar lista de itens.");
                    // Redefinir a fonte no novo stream é essencial
                    currentStream.setFont(FONT_REGULAR, FONT_SIZE_NORMAL);
                }

                if (itemPedido != null && itemPedido.getItem() != null) {
                    // Desenha o item
                    drawText(currentStream, itemCounter + ".", FONT_REGULAR, FONT_SIZE_NORMAL, itemNumberX, currentY);
                    drawText(currentStream, itemPedido.getItem().getNome(), FONT_REGULAR, FONT_SIZE_NORMAL, descriptionX, currentY);
                    drawUnderline(currentStream, descriptionX, currentY - 2, LINE_LENGTH_ITEM_DESC);
                    drawText(currentStream, "Qtd:", FONT_REGULAR, FONT_SIZE_NORMAL, qtyLabelX, currentY);
                    String qtdStr = String.format(Locale.US, "%.2f", itemPedido.getQuantidade());
                    drawText(currentStream, qtdStr, FONT_REGULAR, FONT_SIZE_NORMAL, qtyValueX, currentY);
                    drawUnderline(currentStream, qtyValueX, currentY - 2, LINE_LENGTH_ITEM_QTY);

                    currentY -= ITEM_LINE_SPACING;
                    itemCounter++;
                }
            }
        }

        // Desenha linhas vazias se necessário
        while (itemCounter <= minLines) {
            if (currentY < MARGIN + ITEM_LINE_SPACING) {
                currentStream.close();
                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                currentStream = new PDPageContentStream(document, currentPage);
                currentY = PAGE_HEIGHT - MARGIN;
                System.out.println("Adicionada nova página para linhas vazias de itens.");
                currentStream.setFont(FONT_REGULAR, FONT_SIZE_NORMAL); // Redefine a fonte
            }
            drawText(currentStream, itemCounter + ".", FONT_REGULAR, FONT_SIZE_NORMAL, itemNumberX, currentY);
            drawUnderline(currentStream, descriptionX, currentY - 2, LINE_LENGTH_ITEM_DESC);
            drawText(currentStream, "Qtd:", FONT_REGULAR, FONT_SIZE_NORMAL, qtyLabelX, currentY);
            drawUnderline(currentStream, qtyValueX, currentY - 2, LINE_LENGTH_ITEM_QTY);
            currentY -= ITEM_LINE_SPACING;
            itemCounter++;
        }

        // Retorna o estado atualizado
        return new Object[]{currentPage, currentStream, currentY};
    }

    /** Gera o texto completo dos termos, inserindo o nome do cliente. */
    private String getTermsText(Cliente cliente) {
        String clientNameUpper = (cliente != null && cliente.getNome() != null && !cliente.getNome().isBlank())
                ? cliente.getNome().toUpperCase()
                : "_________________________"; // Placeholder se nome for nulo/vazio

        return "EU " + "________________________________________________________________________________________" +
                " comprometo-me a devolver no dia, os produtos alugados, em perfeitos estados que recebi. No caso de atraso na devolução, estou ciente e de acordo de que será cobrada uma multa de 10% ao dia do valor da locação. Na falta ou danificação dos mesmos, comprometo-me a reembolsar a quantia correspondente ao valor da mercadoria sujeito a taxa de lavagem, valor contado em lavanderias (sujeiras excessivas). Não entregamos produtos a menores de idades. Estou ciente de que tenho 24 horas para desistir da locação, passando este prazo pagarei multa de 25% no valor da locação e não é devolvido o sinal. O não cumprimento deste termo de responsabilidade poderá incluir seu nome para o serviço de proteção ao crédito SPC/SERASA.";
    }

    /** Desenha a seção de termos e a linha de assinatura. */
    private void drawTermsAndSignature(PDPageContentStream stream, Cliente cliente, float yStart) throws IOException {
        float currentY = yStart;
        String termsText = getTermsText(cliente);

        // Desenha o texto dos termos com quebra de linha
        // O método drawWrappedText retorna a posição Y após desenhar o texto
        currentY = drawWrappedText(stream, termsText, FONT_REGULAR, FONT_SIZE_SMALL, MARGIN, currentY, CONTENT_WIDTH, TERMS_LINE_SPACING);

        currentY -= LINE_SPACING * 2; // Espaço extra antes da linha de assinatura

        // --- Linha de Assinatura ---
        // Verifica se a linha cabe (simplificado)
        if (currentY < MARGIN + LINE_SPACING) {
            System.err.println("AVISO: Linha de assinatura pode estar fora da página ou muito baixa.");
            currentY = MARGIN + LINE_SPACING; // Força uma posição mínima
        }

        float signatureLineX = MARGIN + (CONTENT_WIDTH - LINE_LENGTH_SIGNATURE) / 2; // Centralizada
        drawUnderline(stream, signatureLineX, currentY, LINE_LENGTH_SIGNATURE);
    }


    // =========================================================================
    // Métodos Utilitários de Desenho Genéricos
    // =========================================================================

    /** Desenha texto simples em uma posição. */
    private void drawText(PDPageContentStream stream, String text, PDType1Font font, float fontSize, float x, float y) throws IOException {
        if (text == null) text = ""; // Evita NullPointerException
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    /** Desenha texto centralizado horizontalmente na página. */
    private void drawCenteredText(PDPageContentStream stream, String text, PDType1Font font, float fontSize, float y, float pageWidth) throws IOException {
        if (text == null) text = "";
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float textX = (pageWidth - textWidth) / 2;
        if (textX < MARGIN) textX = MARGIN; // Garante margem mínima
        drawText(stream, text, font, fontSize, textX, y);
    }

    /** Desenha uma linha horizontal. */
    private void drawUnderline(PDPageContentStream stream, float x, float y, float length) throws IOException {
        stream.saveGraphicsState(); // Salva estado gráfico (cor, espessura da linha, etc.)
        stream.setLineWidth(0.5f); // Define espessura fina para a linha
        stream.moveTo(x, y);
        stream.lineTo(x + length, y);
        stream.stroke();
        stream.restoreGraphicsState(); // Restaura estado gráfico anterior
    }

    /**
     * Desenha um label seguido por um valor e uma linha abaixo do valor.
     * Usado para campos de largura total ou quando o controle de Y é feito pelo chamador.
     */
    private float drawLabelAndValueWithLine(PDPageContentStream stream, String label, String value, float x, float y, float lineLength) throws IOException {
        drawText(stream, label, FONT_REGULAR, FONT_SIZE_NORMAL, x, y);
        float labelWidth = FONT_REGULAR.getStringWidth(label + " ") / 1000 * FONT_SIZE_NORMAL;
        float valueX = x + labelWidth;
        drawText(stream, value, FONT_REGULAR, FONT_SIZE_NORMAL, valueX, y);
        drawUnderline(stream, valueX, y - 2, lineLength); // Linha ligeiramente abaixo
        return y - LINE_SPACING; // Retorna a próxima posição Y
    }

    /**
     * Desenha dois conjuntos de label/valor na mesma linha, cada um com sua linha.
     */
    private float drawTwoColumnsWithLines(PDPageContentStream stream,
                                          String label1, String value1, float length1,
                                          String label2, String value2, float length2,
                                          float x1, float x2, float y) throws IOException {
        // Coluna 1
        drawText(stream, label1, FONT_REGULAR, FONT_SIZE_NORMAL, x1, y);
        float label1Width = FONT_REGULAR.getStringWidth(label1 + " ") / 1000 * FONT_SIZE_NORMAL;
        float value1X = x1 + label1Width;
        drawText(stream, value1, FONT_REGULAR, FONT_SIZE_NORMAL, value1X, y);
        drawUnderline(stream, value1X, y - 2, length1);

        // Coluna 2
        drawText(stream, label2, FONT_REGULAR, FONT_SIZE_NORMAL, x2, y);
        float label2Width = FONT_REGULAR.getStringWidth(label2 + " ") / 1000 * FONT_SIZE_NORMAL;
        float value2X = x2 + label2Width;
        drawText(stream, value2, FONT_REGULAR, FONT_SIZE_NORMAL, value2X, y);
        drawUnderline(stream, value2X, y - 2, length2);

        return y - LINE_SPACING; // Retorna a próxima posição Y
    }

    /**
     * Desenha um bloco de texto com quebra de linha automática (simples).
     * Retorna a posição Y final após desenhar o texto.
     */
    private float drawWrappedText(PDPageContentStream stream, String text, PDType1Font font, float fontSize, float x, float y, float maxWidth, float leading) throws IOException {
        if (text == null) return y; // Não desenha nada se o texto for nulo

        float currentY = y;
        String[] words = text.split("(?<=\\s)|(?=\\s)"); // Divide por espaços, mantendo-os para cálculo de largura
        StringBuilder line = new StringBuilder();

        stream.setFont(font, fontSize);

        for (String word : words) {
            if (word.trim().isEmpty() && line.length() == 0) continue; // Ignora espaços no início da linha

            float wordWidth = font.getStringWidth(word) / 1000 * fontSize;
            float currentLineWidth = font.getStringWidth(line.toString()) / 1000 * fontSize;

            if (line.length() > 0 && currentLineWidth + wordWidth > maxWidth) {
                // Desenha a linha atual e começa uma nova
                drawText(stream, line.toString().trim(), font, fontSize, x, currentY); // Trim para remover espaço final
                currentY -= leading;
                line = new StringBuilder(word.trim()); // Começa nova linha com a palavra (sem espaço inicial)
                // Adicionar verificação de paginação aqui se necessário
                // if (currentY < MARGIN) { /* código de nova página */ }
            } else {
                line.append(word); // Adiciona palavra (com seu espaço original) à linha
            }
        }
        // Desenha a última linha restante
        if (line.length() > 0) {
            drawText(stream, line.toString().trim(), font, fontSize, x, currentY);
            currentY -= leading; // Desce Y mesmo para a última linha
        }
        return currentY; // Retorna a posição Y após a última linha desenhada
    }

    /** Calcula a altura aproximada que um texto ocupará quando quebrado. */
    private float calculateApproxTextHeight(String text, float maxWidth, PDType1Font font, float fontSize, float leading) throws IOException {
        if (text == null || text.isEmpty()) return 0;

        float totalLength = font.getStringWidth(text) / 1000 * fontSize;
        int approxLines = (int) Math.ceil(totalLength / maxWidth);
        // Adiciona um fator extra, pois a quebra por palavra pode gerar mais linhas
        approxLines = Math.max(1, (int)(approxLines * 1.1)); // Estimativa + 10%
        return approxLines * leading;
    }

}