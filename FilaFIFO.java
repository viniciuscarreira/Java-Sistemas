import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Random;

class Cliente {
    private int id;
    private LocalTime tempoChegada;
    private LocalTime tempoAtendimento;
    private LocalTime tempoSaida;

    public Cliente(int id, LocalTime tempoChegada) {
        this.id = id;
        this.tempoChegada = tempoChegada;
    }

    public int getId() {
        return id;
    }

    public LocalTime getTempoChegada() {
        return tempoChegada;
    }

    public void setTempoAtendimento(LocalTime tempoAtendimento) {
        this.tempoAtendimento = tempoAtendimento;
    }

    public LocalTime getTempoAtendimento() {
        return tempoAtendimento;
    }

    public void setTempoSaida(LocalTime tempoSaida) {
        this.tempoSaida = tempoSaida;
    }

    public LocalTime getTempoSaida() {
        return tempoSaida;
    }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", tempoChegada=" + tempoChegada + ", tempoAtendimento=" + tempoAtendimento + ", tempoSaida=" + tempoSaida + "}";
    }
}

class Evento {
    private String tipo;
    private LocalTime tempo;

    public Evento(String tipo, LocalTime tempo) {
        this.tipo = tipo;
        this.tempo = tempo;
    }

    @Override
    public String toString() {
        return "Evento{tipo=" + tipo + ", tempo=" + tempo + "}";
    }
}

public class FilaFIFO {
    private Queue<Cliente> fila;
    private Queue<Cliente> caixaAtual;
    private Queue<Cliente> proximoCaixa;
    private LocalTime tempoAtual;
    private int clientesAtendidos;
    private Duration esperaTotal;
    private Duration tempoEsperaTotal; // Nova variável para o tempo de espera total
    private long areaSobQt;
    private long areaSobBt; // Área sob B(t)
    private LinkedList<Evento> listaEventos;
    private DateTimeFormatter timeFormatter;
    private int proximoId;
    private Random random;
    private final int TEMPO_MAXIMO_CHEGADA = 5; // Tempo máximo de chegada entre clientes (em minutos)
    private final int TEMPO_MINIMO_ATENDIMENTO = 10; // Tempo mínimo de atendimento no caixa (em minutos)
    private final int TEMPO_MAXIMO_ATENDIMENTO = 45; // Tempo máximo de atendimento no caixa (em minutos)

    public FilaFIFO() {
        this.fila = new LinkedList<>();
        this.caixaAtual = new LinkedList<>();
        this.proximoCaixa = new LinkedList<>();
        this.tempoAtual = LocalTime.of(0, 0); // Inicializa o tempo atual em 00:00
        this.clientesAtendidos = 0;
        this.esperaTotal = Duration.ZERO;
        this.tempoEsperaTotal = Duration.ZERO; // Inicializa o tempo de espera total em 0
        this.areaSobQt = 0;
        this.areaSobBt = 0; // Inicializa a área sob B(t) em 0
        this.listaEventos = new LinkedList<>();
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        this.proximoId = 1; // Inicializa o próximo ID de cliente em 1
        this.random = new Random(); // Inicializa o gerador de números aleatórios
    }

    public void adicionarClientes() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o número de clientes a adicionar: ");
        int numClientes = scanner.nextInt();
        System.out.print("Digite o tempo de chegada do primeiro cliente (HH:MM): ");
        String tempoChegadaStr = scanner.next();
        LocalTime tempoChegada = LocalTime.parse(tempoChegadaStr, timeFormatter);

        for (int i = 0; i < numClientes; i++) {
            Cliente cliente = new Cliente(proximoId++, tempoChegada);
            fila.add(cliente);
            listaEventos.add(new Evento("chegada", tempoChegada));
            System.out.println("Adicionado: " + cliente);

            // Gera um tempo de chegada para o próximo cliente
            int tempoEsperaAleatorio = 1 + random.nextInt(TEMPO_MAXIMO_CHEGADA);
            tempoChegada = tempoChegada.plusMinutes(tempoEsperaAleatorio);
        }
    }

    public void atenderClientes() {
        while (!fila.isEmpty() || !caixaAtual.isEmpty() || !proximoCaixa.isEmpty()) {
            // Atende clientes no caixa atual
            while (!caixaAtual.isEmpty()) {
                Cliente cliente = caixaAtual.poll();
                int tempoAtendimentoAleatorio = TEMPO_MINIMO_ATENDIMENTO + random.nextInt(TEMPO_MAXIMO_ATENDIMENTO - TEMPO_MINIMO_ATENDIMENTO + 1);
                cliente.setTempoSaida(tempoAtual.plusMinutes(tempoAtendimentoAleatorio)); // Define a hora de saída do cliente

                // Calcula o tempo de espera do cliente e acumula
                Duration tempoEspera = Duration.between(cliente.getTempoAtendimento(), cliente.getTempoSaida());
                tempoEsperaTotal = tempoEsperaTotal.plus(tempoEspera);

                tempoAtual = cliente.getTempoSaida(); // Atualiza o tempo atual para o fim do atendimento do cliente

                clientesAtendidos++;

                System.out.println("=========================================");
                System.out.println("Cliente " + cliente.getId());
                System.out.println("Hora de chegada: " + cliente.getTempoChegada().format(timeFormatter));
                System.out.println("Início do atendimento: " + cliente.getTempoAtendimento().format(timeFormatter));
                System.out.println("Hora de saída: " + cliente.getTempoSaida().format(timeFormatter));
                System.out.println("Tempo de atendimento: " + tempoAtendimentoAleatorio + " minutos");
                System.out.println("Tempo de espera: " + tempoEspera.toHours() + ":" + String.format("%02d", tempoEspera.toMinutes() % 60));
                System.out.println("=========================================");
            }

            // Se não há clientes no caixa atual e há clientes na fila principal, transfere para o caixa atual
            if (caixaAtual.isEmpty() && !fila.isEmpty()) {
                Cliente cliente = fila.poll();
                cliente.setTempoAtendimento(tempoAtual); // Atendimento começa no horário atual do sistema
                caixaAtual.add(cliente);
                listaEventos.add(new Evento("atendimento", cliente.getTempoAtendimento()));

                System.out.println("=========================================");
                System.out.println("Cliente " + cliente.getId());
                System.out.println("Hora de chegada: " + cliente.getTempoChegada().format(timeFormatter));
                System.out.println("Início do atendimento: " + cliente.getTempoAtendimento().format(timeFormatter));
                System.out.println("=========================================");
            }

            // Verifica se algum cliente na fila está esperando há mais de 30 minutos para abrir um novo caixa
            abrirNovoCaixa();

            // Troca de caixa: caixa atual se torna o próximo caixa, e o próximo caixa se torna o caixa atual
            Queue<Cliente> temp = caixaAtual;
            caixaAtual = proximoCaixa;
            proximoCaixa = temp;

            // Avança o tempo apenas se houver clientes na fila principal ou nos caixas
            if (!fila.isEmpty() || !caixaAtual.isEmpty()) {
                passarTempo(1);
            }
        }
        System.out.println("Todos os clientes foram atendidos.");
    }

    private void abrirNovoCaixa() {
        // Verifica se há algum cliente na fila esperando há mais de 30 minutos
        for (Cliente cliente : fila) {
            Duration tempoEspera = Duration.between(cliente.getTempoChegada(), tempoAtual);
            if (tempoEspera.toMinutes() >= TEMPO_MAXIMO_ATENDIMENTO) {
                // Move o cliente para o próximo caixa
                proximoCaixa.add(cliente);
                System.out.println("Cliente " + cliente.getId() + " foi encaminhado para um novo caixa devido ao tempo de espera.");
                fila.remove(cliente);
            }
        }
    }

    public void passarTempo(int unidades) {
        areaSobQt += fila.size() * unidades;
        areaSobBt += caixaAtual.size() * unidades; // Corrigido para refletir o tamanho do caixa atual
        tempoAtual = tempoAtual.plusMinutes(unidades);
    }

    public void imprimirEstatisticas() {
        long hours = tempoEsperaTotal.toHours();
        long minutes = tempoEsperaTotal.toMinutes() % 60;
        System.out.println("\nEstatísticas:");
        System.out.println("Clientes atendidos: " + clientesAtendidos);
        System.out.println("Área sob Q(t): " + areaSobQt);
        System.out.println("Área sob B(t): " + areaSobBt);
        System.out.println("Tempo de espera total: " + hours + ":" + String.format("%02d", minutes));
    }

    public static void main(String[] args) {
        FilaFIFO filaFIFO = new FilaFIFO();
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;

        while (continuar) {
            System.out.println("\n1. Adicionar Clientes");
            System.out.println("2. Atender Clientes");
            System.out.println("3. Imprimir Estatísticas");
            System.out.println("4. Sair");
            System.out.print("Escolha uma opção: ");
            int opcao = scanner.nextInt();

            switch (opcao) {
                case 1:
                    filaFIFO.adicionarClientes();
                    break;
                case 2:
                    filaFIFO.atenderClientes();
                    break;
                case 3:
                    filaFIFO.imprimirEstatisticas();
                    break;
                case 4:
                    continuar = false;
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
                    break;
            }
        }

        scanner.close();
    }
}
