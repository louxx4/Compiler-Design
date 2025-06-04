.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
mov $2, %r14d
mov $2, %cl
sal %r14d
mov %r14d, %eax
cltq
ret
