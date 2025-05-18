
![TPSSSSSSS](https://github.com/user-attachments/assets/418239ef-e1c4-49ec-a898-6a9e7a63b74f)




# Spigot-1.20.5
 
 
 Spigot Modificado alguns comandos como /tps entre outros!


EnderChestPlus BETA
Status: Em desenvolvimento

![EnderChestBeta1 21 5](https://github.com/user-attachments/assets/b6fd3996-35eb-473a-ae64-df9b46d9dcb8)




Este projeto ainda está em fase BETA e não está concluído. Atualmente, apenas o código de abertura do baú foi implementado.
Foram feitas alterações na source do Spigot, mas o NNS do cliente ainda não foi modificado.

⚠️ Aviso: Este código é temporário e acessa diretamente o Ender Chest real do jogador utilizando a API padrão do Spigot.
No futuro, será substituído por um sistema próprio de inventário, integrado diretamente à source do Spigot modificada.
Essa funcionalidade já está em desenvolvimento e, quando finalizada, não dependerá mais de plugins externos para funcionar.

@EventHandler
public void onEnderChestOpen(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
        event.getClickedBlock() != null &&
        event.getClickedBlock().getType() == Material.ENDER_CHEST) {

        // Cancela a abertura padrão do baú
        event.setCancelled(true);

        Player player = event.getPlayer();
        Inventory realEnderChest = player.getEnderChest(); // Ender Chest real do jogador

        // Toca o som de abertura do Ender Chest
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);

        // Abre o inventário real do Ender Chest
        // OBS: Se a source já estiver modificada, pode ter 54 slots
        player.openInventory(realEnderChest);
    }
}

@EventHandler
public void onEnderChestClose(InventoryCloseEvent event) {
    if (event.getInventory().equals(event.getPlayer().getEnderChest())) {
        Player player = (Player) event.getPlayer();

        // Toca o som de fechamento do Ender Chest
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f);
    }
}

