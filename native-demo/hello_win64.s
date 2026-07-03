    .section .rdata,"dr"
message:
    .asciz "Hello from Lemon native x64!"

    .text
    .globl main
main:
    pushq %rbp
    movq %rsp, %rbp

    # Windows x64 ABI: reserve 32 bytes of shadow space before calls.
    subq $32, %rsp

    # First integer/pointer argument goes in RCX on Windows x64.
    leaq message(%rip), %rcx
    call puts

    xorl %eax, %eax
    addq $32, %rsp
    popq %rbp
    ret
