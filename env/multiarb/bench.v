`define SRC_PAT 16'hFFFF

module bench;

  reg clock, reset;
  reg [15:0] io_src_pat, io_dst_pat;
  reg 	     io_enable;

  reg [2:0]  io_dst[0:7];
  wire [31:0] io_cum_delay[0:7];
  /*AUTOWIRE*/
  // Beginning of automatic wires (for undeclared instantiated-module outputs)
  wire			io_addr_error;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_0;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_1;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_2;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_3;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_4;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_5;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_6;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_cum_latency_7;	// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_0;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_1;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_2;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_3;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_4;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_5;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_6;		// From dut of MultiArbTestbench.v
  wire [31:0]		io_pkt_count_7;		// From dut of MultiArbTestbench.v
  // End of automatics

 /* MultiArbTestbench AUTO_TEMPLATE
  (
   .io_dst_\([0-9]+\) (io_dst[\1]),
   .io_cum_delay_\([0-9]+\) (io_cum_delay[\1]),
  )
  */  
  MultiArbTestbench dut
    (/*AUTOINST*/
     // Outputs
     .io_addr_error			(io_addr_error),
     .io_cum_latency_0			(io_cum_latency_0[31:0]),
     .io_cum_latency_1			(io_cum_latency_1[31:0]),
     .io_cum_latency_2			(io_cum_latency_2[31:0]),
     .io_cum_latency_3			(io_cum_latency_3[31:0]),
     .io_cum_latency_4			(io_cum_latency_4[31:0]),
     .io_cum_latency_5			(io_cum_latency_5[31:0]),
     .io_cum_latency_6			(io_cum_latency_6[31:0]),
     .io_cum_latency_7			(io_cum_latency_7[31:0]),
     .io_pkt_count_0			(io_pkt_count_0[31:0]),
     .io_pkt_count_1			(io_pkt_count_1[31:0]),
     .io_pkt_count_2			(io_pkt_count_2[31:0]),
     .io_pkt_count_3			(io_pkt_count_3[31:0]),
     .io_pkt_count_4			(io_pkt_count_4[31:0]),
     .io_pkt_count_5			(io_pkt_count_5[31:0]),
     .io_pkt_count_6			(io_pkt_count_6[31:0]),
     .io_pkt_count_7			(io_pkt_count_7[31:0]),
     .io_cum_delay_0			(io_cum_delay[0]),	 // Templated
     .io_cum_delay_1			(io_cum_delay[1]),	 // Templated
     .io_cum_delay_2			(io_cum_delay[2]),	 // Templated
     .io_cum_delay_3			(io_cum_delay[3]),	 // Templated
     .io_cum_delay_4			(io_cum_delay[4]),	 // Templated
     .io_cum_delay_5			(io_cum_delay[5]),	 // Templated
     .io_cum_delay_6			(io_cum_delay[6]),	 // Templated
     .io_cum_delay_7			(io_cum_delay[7]),	 // Templated
     // Inputs
     .clock				(clock),
     .reset				(reset),
     .io_src_pat			(io_src_pat[15:0]),
     .io_dst_pat			(io_dst_pat[15:0]),
     .io_enable				(io_enable),
     .io_dst_0				(io_dst[0]),		 // Templated
     .io_dst_1				(io_dst[1]),		 // Templated
     .io_dst_2				(io_dst[2]),		 // Templated
     .io_dst_3				(io_dst[3]),		 // Templated
     .io_dst_4				(io_dst[4]),		 // Templated
     .io_dst_5				(io_dst[5]),		 // Templated
     .io_dst_6				(io_dst[6]),		 // Templated
     .io_dst_7				(io_dst[7]));		 // Templated

  task print_stats;
    integer i;
    begin
      $display("STAT latency %d %d", 0, io_cum_latency_0);
      $display("STAT latency %d %d", 1, io_cum_latency_1);
      $display("STAT latency %d %d", 2, io_cum_latency_2);
      $display("STAT latency %d %d", 3, io_cum_latency_3);
      $display("STAT latency %d %d", 4, io_cum_latency_4);
      $display("STAT latency %d %d", 5, io_cum_latency_5);
      $display("STAT latency %d %d", 6, io_cum_latency_6);
      $display("STAT latency %d %d", 7, io_cum_latency_7);
      $display("STAT count %d %d", 0, io_pkt_count_0);
      $display("STAT count %d %d", 1, io_pkt_count_1);
      $display("STAT count %d %d", 2, io_pkt_count_2);
      $display("STAT count %d %d", 3, io_pkt_count_3);
      $display("STAT count %d %d", 4, io_pkt_count_4);
      $display("STAT count %d %d", 5, io_pkt_count_5);
      $display("STAT count %d %d", 6, io_pkt_count_6);
      $display("STAT count %d %d", 7, io_pkt_count_7);
      for (i=0; i<8; i=i+1)
	$display("STAT delay %d %d", i, io_cum_delay[i]);
    end
  endtask // print_stats
  
  initial
    begin
      $dumpfile("bench.vcd");
      $dumpvars;
      reset = 1;
      io_src_pat = `SRC_PAT;
      io_dst_pat = 16'hFFFF;
      io_enable = 0;
      #100;
      io_enable = 1;
      reset = 0;
      #10000;
      io_enable = 0;
      #100;
      print_stats;
      $finish;
    end

  always
    begin
      clock = 0;
      #5;
      clock = 1;
      #5;
    end

  always @(posedge clock)
    begin : rdest
      integer i;
      for (i=0; i<8; i=i+1)
	io_dst[i] = $random;
    end
  
  
endmodule // bench
