module DCOutputTestbenchHarness( // @[:@136.2]
  input         clock, // @[:@137.4]
  input         reset, // @[:@138.4]
  input  [15:0] io_src_pat, // @[:@139.4]
  input  [15:0] io_dst_pat, // @[:@139.4]
  output        io_color_error, // @[:@139.4]
  output        io_seq_error // @[:@139.4]
);

 DCOutputTestbench dut
(
 clock, // @[:@137.4]
 reset, // @[:@138.4]
 io_src_pat, // @[:@139.4]
 io_dst_pat, // @[:@139.4]
 io_color_error, // @[:@139.4]
 io_seq_error // @[:@139.4]
);

  assert property (io_seq_error == 1'b0);
  assert property (io_color_error == 1'b0);

endmodule
