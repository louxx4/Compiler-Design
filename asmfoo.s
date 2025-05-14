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
mov 100, %r12
imul %r12, 10
mov 40, %r12
add %r12, %r12
mov %rax, %r12
ret
